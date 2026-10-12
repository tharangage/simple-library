// Runtime settings. Empty here: `npm run dev` and `npm run build` take the values from the VITE_* variables
// in .env. In the Docker image, docker/40-runtime-config.sh overwrites this file when the container starts,
// so one image can run against any API and Keycloak. See docs/setup/docker-hub.md.
window.APP_CONFIG = {}
