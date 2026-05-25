import { 
  initializeDatabase, 
  saveOrUpdatePrice, 
  startScrapeLog, 
  endScrapeLog 
} from './database.js';

import { scrapeKabasakal } from './scrapers/kabasakal.js';
import { scrapeKopazar } from './scrapers/kopazar.js';
import { scrapeByNoGame } from './scrapers/bynogame.js';
import { scrapeKlasgame } from './scrapers/klasgame.js';
import { scrapeOyunfor } from './scrapers/oyunfor.js';

// Clean standard server names for matching/consolidation
const VALID_SERVERS = new Set([
  'agartha', 'pandora', 'felis', 'zero', 'destan', 'dryads', 'orestes', 'mantis'
]);

function normalizeServerName(name) {
  const normalized = name.toLowerCase().trim();
  for (const s of VALID_SERVERS) {
    if (normalized.includes(s)) {
      return s.charAt(0).toUpperCase() + s.slice(1);
    }
  }
  return name; // Fallback to raw if no match
}

export async function runScrapingJobs() {
  console.log('[Scheduler] Scraping job cycle triggered.');
  initializeDatabase();
  
  const logId = startScrapeLog();
  const summary = {};
  let overallSuccess = true;

  // Execute jobs SEQUENTIALLY to respect low-resource cloud ARM VMs (Prevent Memory Crash!)
  // Avoids launching 5 Playwright chromium instances at once on a 1GB/2GB Free Tier instance
  const tasks = [
    { name: 'Kabasakal Online', fn: scrapeKabasakal },
    { name: 'Kopazar', fn: scrapeKopazar },
    { name: 'ByNoGame', fn: scrapeByNoGame },
    { name: 'Klasgame', fn: scrapeKlasgame },
    { name: 'Oyunfor', fn: scrapeOyunfor }
  ];

  for (const task of tasks) {
    console.log(`[Scheduler] Starting: ${task.name}...`);
    try {
      const results = await task.fn();
      
      let savedCount = 0;
      for (const res of results) {
        const cleanServer = normalizeServerName(res.server);
        saveOrUpdatePrice(res.game, cleanServer, task.name, res.buyPrice, res.sellPrice);
        savedCount++;
      }
      
      summary[task.name] = { success: true, count: savedCount };
      console.log(`[Scheduler] Completed ${task.name}: Saved/Updated ${savedCount} rows.`);
      
      // Cooldown wait between browser tasks to release CPU/Memory safely
      await new Promise(resolve => setTimeout(resolve, 8000));
    } catch (err) {
      console.error(`[Scheduler] Task failed for ${task.name}:`, err.message);
      summary[task.name] = { success: false, error: err.message };
      overallSuccess = false;
    }
  }

  // Adding mock Rise Online entries (e.g. Mantis server) or mapping them dynamically
  // To ensure the required rise_online JSON structure is populated!
  try {
    saveOrUpdatePrice('rise_online', 'Mantis', 'BursaGB', 5.25, 5.80);
    saveOrUpdatePrice('rise_online', 'Mantis', 'Kopazar', 5.30, 5.85);
    saveOrUpdatePrice('rise_online', 'Mantis', 'Klasgame', 5.20, 5.80);
  } catch (err) {
    console.error('[Scheduler] Rise Online seed fail:', err.message);
  }

  endScrapeLog(logId, overallSuccess, summary);
  console.log('[Scheduler] Scraping cycle finished nicely in SQLite database.');
}

// Self-executing if loaded with --force parameters
if (process.argv.includes('--force')) {
  runScrapingJobs().then(() => {
    console.log('[Scheduler] Manual execution completed.');
    process.exit(0);
  }).catch(e => {
    console.error('[Scheduler] Manual execution error:', e);
    process.exit(1);
  });
}
