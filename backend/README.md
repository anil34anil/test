# 🎯 Gold Bar (GB) Scraper & Backend API Setup Guide
### Highly Optimized for Oracle Cloud Free Tier (Ampere A1 ARM) • Ubuntu 22.04 LTS

This codebase provides a highly optimized, production-ready, bulletproof Node.js + Playwright backend scraper and Express REST API. It is specifically configured to survive the **memories and firewall restrictions of Oracle Cloud Free Tier VMs**, utilizing extremely lightweight design choices.

---

## 🛠️ Step-by-Step Oracle Cloud Setup & Deployment

Oracle Cloud Infrastructure (OCI) Free Tier offers powerful Ampere A1 ARM instances. However, they come with high firewall restrictions (both in the virtual cloud network dashboard and internal Ubuntu images) and memory limit challenges. Follow these precise instructions to deploy successfully.

### 1. Configure OCI Security Lists (Oracle Dashboard)
By default, Oracle Cloud blocks all incoming traffic. You must authorize Ports `80` and `443` inside your virtual dashboard:
1. Log in to the **Oracle Cloud Console**.
2. Go to **Networking** ➔ **Virtual Cloud Networks**.
3. Select your VCN and click on **Default Security List for...**.
4. Click **Add Ingress Rules** and submit:
   * **Source Type:** CIDR
   * **Source CIDR:** `0.0.0.0/0`
   * **IP Protocol:** `TCP`
   * **Destination Port Range:** `80,443,8080`
   * **Description:** Allow HTTP/HTTPS and Node service traffic.

### 2. Prepare the Ubuntu OS (Terminal Level)
Ubuntu instances on OCI run standard Linux firewalls plus custom strict `iptables` configuration rules that block Node ports on startup. Execute these commands to flush them and install prerequisites:

```bash
# Update local packages
sudo apt update && sudo apt upgrade -y

# CRITICAL: Flush Oracle's restrictive iptables rules to allow actual network traffic!
# (Many developers struggle with OCI because UFW works, but iptables still blocks ports!)
sudo iptables -F
sudo iptables-save | sudo tee /etc/iptables/rules.v4
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 8080/tcp
sudo ufw reload

# Install Node.js (Node 20 LTS version)
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# Verify installations
node -v
npm -v
```

### 3. Setup Project with Playwright Dependencies
Fetch your backend project files into the VM and configure playwright dependencies:

```bash
# Navigate to the backend directory
cd /home/ubuntu/gb-market-backend

# Install package dependencies
npm install

# Install Chromium binaries for ARM and required system shared libraries (libgbm / fonts)
npx playwright install --with-deps chromium
```

### 4. Configure Production Environment
Create a `.env` configuration file inside `/backend` directory:
```bash
nano .env
```
Paste and fill inside:
```env
PORT=8080
NODE_ENV=production
ADMIN_SECRET=your_super_strong_custom_bearer_token
```

### 5. Running with PM2 (Process Manager)
Configure PM2 process manager tool to ensure automatic recovery on server restarts, unexpected application errors, and to prevent RAM leakage in Free Tier:

```bash
# Install PM2 globally
sudo npm install pm2 -g

# Start the application using our optimized ARM Ecosystem configurations
pm2 start ecosystem.config.cjs

# Setup system boot configuration hook
pm2 startup
# (Run the output hook command displayed in your terminal terminal to enable auto-restart)

# Save current active configuration layout
pm2 save
```

### 6. Set Up Nginx & SSL (HTTPS Encryption)
Never expose Port `8080` directly to public mobile configurations. Wrap it Behind **Nginx Reverse Proxy** with a free Let's Encrypt SSL certificate.

```bash
# Install Nginx
sudo apt install nginx -y

# Open Nginx Virtual Host config file
sudo nano /etc/nginx/sites-available/gb-market

# Paste the reverse-proxy configuration block below:
server {
    listen 80;
    server_name yourdomain.com; # Point your target DNS / Subdomain A record to VM Public IP

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# Enable config and restart Nginx
sudo ln -s /etc/nginx/sites-available/gb-market /etc/nginx/sites-enabled/
sudo rm /etc/nginx/sites-enabled/default
sudo systemctl restart nginx

# Install Let's Encrypt Certbot
sudo apt install certbot python3-certbot-nginx -y

# Obtain free SSL Certificate and enable auto-renewals in seconds!
sudo certbot --nginx -d yourdomain.com
```

---

## 🛡️ Overcoming Cloudflare & Anti-Bot Protections Legally and Safely

Turkish marketplaces use Cloudflare, Datadome, or direct web application firewalls (WAF) to defend themselves against heavy DDoS attacks and scrapers. Use these strategies inside our scraper configurations:

### 1. Direct Hidden JSON API Prioritization (Recommended)
This codebase uses the **hybrid strategy**: It first queries public endpoints and internal XHR calls (e.g., product feeds, catalog search arrays, dynamic product cards widgets inside search engines). These endpoints usually bypass strict security rules and deliver responses in $10\text{ms}$ with **0% server overhead**.

### 2. Playwright Stealth Plugin (`puppeteer-extra-plugin-stealth` in Playwright)
If direct APIs block request parameters, Playwright launches Chromium running stealth modules:
* Bypasses `navigator.webdriver` exposure.
* Mocks original screen metrics, CSS formats, and browser plugin support arrays.
* Bypasses WebGL fingerprint indicators.

### 3. Human Performance Mimicry
Scraping bots fail when request velocities look unnatural. Our scrapers include:
* **Random Cooldown Intervals:** A `8000ms` cooldown wait stage exists between each server scraper run. This ensures that the VM never crashes due to memory overflow and source platforms do not detect uniform traffic spikes.
* **Scroll Patterns:** Simulating random mouse scroll down behaviors (`page.mouse.wheel`) triggers rendering mechanisms naturally just like real gamers.

### 4. Residential Proxies Integration (For Scaled Deployment)
To prevent your Oracle Cloud IPv4 address from getting clean-blocked by Cloudflare:
Register a fast, cheap residential proxy provider (e.g., Bright Data, Oxylabs, ProxyEmpire) and configure Playwright browser contexts inside your scrapers to cycle residential proxies:

```javascript
const browser = await chromium.launch({
  proxy: {
    server: 'http://your-residential-proxy-endpoint:8000',
    username: 'your-proxy-username',
    password: 'your-proxy-password'
  }
});
```

---

## 📱 Mobile Architecture & Development Recommendations

For structured cross-platform mobile apps tracking live prices, here is our senior architectural layout advisory:

### 1. Flutter vs. React Native Review

| feature | Flutter (🏆 Recommended) | React Native |
| :--- | :--- | :--- |
| **Performance** | Native Compiled High-Performance Skia/Impeller renderer. Smooth scroll arrays. | JS-to-Native bridge latency, slower scroll on heavy list views. |
| **UI Uniformity** | Pixel-perfect identical layout renderings on both iOS and Android. | Native UI wrappers change visual styles depending on OS models. |
| **Developer Experience** | Fast hot reload, standardized IDE tooling widgets, great compilation. | Node ecosystem package version mismatches can break build outputs. |
| **State Management** | **Bloc Pattern** or **Riverpod** is clean, scalable, and easy to maintain. | Redux is verbose; Zustand or MobX are alternatives but can feel unguided. |

### 2. State Management Recommendations (Flutter)
Use **Riverpod** if your developer team wants clean, declarative code, or **BLoc (Business Logic Component)** if you want standard enterprise clean code.
* **Repository Pattern:** Create a `GbRepository` that wraps your Node.js endpoint and yields `ServerPrices` models.
* **Notifier/State Stage:** Create a `GbPriceNotifier` that handles `Loading`, `Success`, and `Error` states.
* **Automatic Pull-To-Refresh:** Allow players to swipe down to run manual fetches instantly.

### 3. Securing Your Mobile Backend API
Exposing public endpoints without authenticating mobile callers lets competitor scrapers steal your parsed data in seconds!
1. **Dynamic App Attest / Device Check:** Authenticate using Google Play Integrity APIs (Android) and DeviceCheck / App Attest APIs (iOS) to verify that queries only arrive from original genuine mobile applications.
2. **Encrypted Gateway Strings:** Include encrypted headers containing matching static tokens like SHA256 hashes inside endpoints requests.
3. **Strict Rate-Limits (Helmet / express-rate-limit):** Installed and configured in our code to lock down aggressive API abusers.

---

## 🌱 Scaled Architecture for Future Growth

When your app grows from 1,000 to 100,000 daily active gamers:
1. **Add Redis Caching layer:** Instead of fetching directly from SQLite on every client HTTP payload, cache API output directly in a fast, in-memory **Redis** instance with a $5\text{s}$ expiration window.
2. **Decouple API and Scraper VM Engines:** Move Playwright scrapers entirely to an independent, ephemeral container worker or Cloudflare Worker/Serverless setup that publishes records to your database. Keep the Express API running on a tiny $5$ microinstance focused purely on rapid HTTP deliveries.
3. **Database Migration:** Easily migrate standard WAL SQLite files to **PostgreSQL** or **CockroachDB** when handling millions of write operations!
