import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:3000',
    viewportWidth: 1440,
    viewportHeight: 900,
    video: false,
    retries: { runMode: 2, openMode: 0 },
    defaultCommandTimeout: 10_000,
  },
});
