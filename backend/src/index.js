import express from 'express';
import helmet from 'helmet';
import cors from 'cors';
import rateLimit from 'express-rate-limit';
import dotenv from 'dotenv';
import apiRoutes from './routes.js';
import { initializeDatabase } from './database.js';
import { runScrapingJobs } from './scheduler.js';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 8080;

// Security and visual shields
app.use(helmet());
app.use(cors({ origin: '*' })); // Allow cross-platform mobile requests freely
app.use(express.json());

// Strict Rate Limiting (Prevents spamming on API endpoints and preserves VM from DDoS)
const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 300, // Limit each IP to 300 requests per 15 mins
  message: { error: 'Too many requests, please try again later.' },
  standardHeaders: true,
  legacyHeaders: false,
});
app.use('/api', apiLimiter);

// Root path diagnostic
app.get('/', (req, res) => {
  res.json({
    name: 'Knight & Rise Online GB Price Scraper backend API',
    status: 'online',
    systemTime: new Date().toISOString()
  });
});

// Bind APIs
app.use('/api', apiRoutes);

// Database Auto-boots and performs the initial seeding run
initializeDatabase();

// Run Initial scraping trigger on boot to avoid empty states
console.log('[App] Bootstrapping completed. Executing first scraping job in background...');
runScrapingJobs()
  .then(() => console.log('[App] Initial scraping cycle completed on boot!'))
  .catch((e) => console.error('[App] Initial scraping cycle on boot failed:', e));

// Setup automatic periodic refresh every 10 minutes (Custom robust lightweight cron implementation)
const SCRAPING_INTERVAL_MS = 10 * 60 * 1000; // 10 minutes
setInterval(async () => {
  console.log('[Interval Service] Executing 10-minute periodic refresh job...');
  try {
    await runScrapingJobs();
  } catch (err) {
    console.error('[Interval Service] Scrape fail:', err);
  }
}, SCRAPING_INTERVAL_MS);

// Start listening
app.listen(PORT, '0.0.0.0', () => {
  console.log(`=======================================================`);
  console.log(`🚀 API Server Listening securely on: http://0.0.0.0:${PORT}`);
  console.log(`🛡️ Rate Limiter Active: IP Max 300 req / 15 minutes`);
  console.log(`🕒 Automatic periodic scraper active every 10 minutes`);
  console.log(`=======================================================`);
});
