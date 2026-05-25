import express from 'express';
import { getAllLatestPrices, getPriceHistory, runScrapingJobs } from './database.js';

const router = express.Router();

/**
 * REST API: Get Standardized Markets Data
 * Returns JSON in the custom required format
 */
router.get('/prices', (req, res) => {
  try {
    const rawPrices = getAllLatestPrices();
    
    // Group prices by game as specified
    const games = {
      knight_online: [],
      rise_online: []
    };

    let latestUpdate = new Date();

    rawPrices.forEach(p => {
      const entry = {
        source: p.source,
        server: p.server,
        buyPrice: p.buyPrice,
        sellPrice: p.sellPrice,
        currency: p.currency || 'TRY'
      };

      if (p.game === 'knight_online') {
        games.knight_online.push(entry);
      } else if (p.game === 'rise_online') {
        games.rise_online.push(entry);
      }

      // Track last updated node
      const recordDate = new Date(p.updatedAt);
      if (recordDate < latestUpdate) {
        latestUpdate = recordDate;
      }
    });

    return res.json({
      updatedAt: latestUpdate.toISOString(),
      games: games
    });
  } catch (error) {
    console.error('[API] Error rendering prices:', error);
    return res.status(500).json({ error: 'Internal Server Error' });
  }
});

/**
 * Query History for custom analytical charts on mobile
 */
router.get('/history/:game/:server/:source', (req, res) => {
  const { game, server, source } = req.params;
  try {
    const history = getPriceHistory(game, server, source);
    return res.json({
      game,
      server,
      source,
      history
    });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

/**
 * Force manual scraping trigger via Admin Token (protected)
 */
router.post('/scrape/trigger', async (req, res) => {
  const authHeader = req.headers.authorization;
  const adminSecret = process.env.ADMIN_SECRET || 'super-secret-admin-token';

  if (!authHeader || authHeader !== `Bearer ${adminSecret}`) {
    return res.status(401).json({ error: 'Unauthorized manual scraping trigger' });
  }

  // Run in background so request does not timeout
  runScrapingJobs()
    .then(() => console.log('[API] Background manual trigger completed.'))
    .catch(err => console.error('[API] Background manual trigger fail:', err));

  return res.json({ 
    success: true, 
    message: 'Manual scraping trigger initialized asynchronously.' 
  });
});

export default router;
