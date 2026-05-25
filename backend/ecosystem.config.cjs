module.exports = {
  apps: [
    {
      name: 'gb-market-api',
      script: './src/index.js',
      instances: 1, // Single instance is best to conserve RAM on Oracle Free Tier ARM
      exec_mode: 'fork',
      watch: false,
      max_memory_restart: '450M', // Guard against Node memory leaks on 1GB/2GB VMs
      env: {
        NODE_ENV: 'production',
        PORT: 8080,
        ADMIN_SECRET: 'super-secret-admin-token'
      }
    }
  ]
};
