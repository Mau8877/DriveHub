// Proxy del dev server: /api del shell -> API Gateway. Así el navegador
// habla siempre con el mismo origen que el shell (sin CORS), igual que
// detrás de nginx. En Docker, API_GATEWAY_URL apunta al contenedor del gateway.
export default {
  '/api': {
    target: process.env.API_GATEWAY_URL ?? 'http://localhost:8080',
    changeOrigin: true,
  },
}
