import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    global: 'globalThis',
  },
  // server holds dev-server options (port, HTTPS, proxy, etc.).
  server: {
    //  Defines which paths are forwarded and to which target. You can define multiple rules.
    // server.proxy	开发时由 Vite 把部分请求转到后端	Forward some paths to the backend in dev
    proxy: {
      // The rule key '/api' matches paths starting with /api (e.g. /api/auth/login).
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',  // WebSocket 也转发到后端
        changeOrigin: true,
        ws: true,                          // 关键！必须 true，否则 WS 升级失败
      },
    }
  }
})
