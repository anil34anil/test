import Database from 'better-sqlite3';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const dbPath = path.resolve(__dirname, '../data/gb_market.db');

// Ensure database directory exists
import fs from 'fs';
const dir = path.dirname(dbPath);
if (!fs.existsSync(dir)){
    fs.mkdirSync(dir, { recursive: true });
}

const db = new Database(dbPath, { verbose: null });
db.pragma('journal_mode = WAL'); // High-performance WAL mode
db.pragma('synchronous = NORMAL');

// Initialize database schema
export function initializeDatabase() {
  // Create tables for prices
  db.exec(`
    CREATE TABLE IF NOT EXISTS prices (
      id TEXT PRIMARY KEY,
      game TEXT NOT NULL,         -- 'knight_online' or 'rise_online'
      server_name TEXT NOT NULL,  -- e.g. 'Agartha', 'Mantis'
      source_name TEXT NOT NULL,  -- e.g. 'Kopazar', 'Klasgame'
      buy_price REAL NOT NULL,    -- Site purchase price
      sell_price REAL NOT NULL,   -- Site selling price
      currency TEXT DEFAULT 'TRY',
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS price_history (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      game TEXT NOT NULL,
      server_name TEXT NOT NULL,
      source_name TEXT NOT NULL,
      buy_price REAL NOT NULL,
      sell_price REAL NOT NULL,
      recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS scraping_logs (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      finished_at TIMESTAMP,
      sources_scraped TEXT,       -- JSON string of status per source
      success BOOLEAN DEFAULT 0,
      error_message TEXT
    );

    CREATE UNIQUE INDEX IF NOT EXISTS idx_prices_composite 
    ON prices (game, server_name, source_name);
    
    CREATE INDEX IF NOT EXISTS idx_history_composite 
    ON price_history (game, server_name, source_name, recorded_at);
  `);

  console.log(`[Database] SQLite Database initialized securely at: ${dbPath}`);
}

// Queries
export function saveOrUpdatePrice(game, server, source, buy, sell) {
  const checkStmt = db.prepare(`
    SELECT buy_price, sell_price FROM prices 
    WHERE game = ? AND server_name = ? AND source_name = ?
  `);
  
  const existing = checkStmt.get(game, server, source);

  // Parse strings directly to clean rounded floats
  const cleanBuy = parseFloat(buy.toString().replace(/[^0-9.]/g, '')) || 0;
  const cleanSell = parseFloat(sell.toString().replace(/[^0-9.]/g, '')) || 0;

  if (cleanBuy <= 0 || cleanSell <= 0) return;

  const insertStmt = db.prepare(`
    INSERT INTO prices (id, game, server_name, source_name, buy_price, sell_price, updated_at)
    VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
    ON CONFLICT(game, server_name, source_name) DO UPDATE SET
      buy_price = excluded.buy_price,
      sell_price = excluded.sell_price,
      updated_at = CURRENT_TIMESTAMP
  `);

  const id = `${game}_${server}_${source}`.toLowerCase().replace(/\s+/g, '_');
  insertStmt.run(id, game, server, source, cleanBuy, cleanSell);

  // Save history only if values changed to optimize db size
  if (!existing || existing.buy_price !== cleanBuy || existing.sell_price !== cleanSell) {
    const historyStmt = db.prepare(`
      INSERT INTO price_history (game, server_name, source_name, buy_price, sell_price)
      VALUES (?, ?, ?, ?, ?)
    `);
    historyStmt.run(game, server, source, cleanBuy, cleanSell);
  }
}

export function getAllLatestPrices() {
  const stmt = db.prepare(`
    SELECT game, server_name as server, source_name as source, 
           buy_price as buyPrice, sell_price as sellPrice, currency, updated_at as updatedAt
    FROM prices
    ORDER BY game, server_name, buy_price ASC
  `);
  return stmt.all();
}

export function getPriceHistory(game, server, source, limit = 24) {
  const stmt = db.prepare(`
    SELECT buy_price as buyPrice, sell_price as sellPrice, recorded_at as time
    FROM price_history
    WHERE game = ? AND server_name = ? AND source_name = ?
    ORDER BY recorded_at DESC
    LIMIT ?
  `);
  return stmt.all(game, server, source).reverse();
}

export function startScrapeLog() {
  const stmt = db.prepare(`INSERT INTO scraping_logs (started_at) VALUES (CURRENT_TIMESTAMP)`);
  const result = stmt.run();
  return result.lastInsertRowid;
}

export function endScrapeLog(logId, success, sourcesSummary, errorMsg = null) {
  const stmt = db.prepare(`
    UPDATE scraping_logs 
    SET finished_at = CURRENT_TIMESTAMP, success = ?, sources_scraped = ?, error_message = ?
    WHERE id = ?
  `);
  stmt.run(success ? 1 : 0, JSON.stringify(sourcesSummary), errorMsg, logId);
}

export default db;
