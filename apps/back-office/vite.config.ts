import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// 3001: the web app owns 3000 (see fikaliako.cors.allowed-origins in the API).
export default defineConfig({
  plugins: [react()],
  server: { port: 3001 },
  preview: { port: 3001 },
});
