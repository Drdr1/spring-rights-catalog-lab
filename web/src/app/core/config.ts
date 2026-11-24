/** Runtime settings. Values match docker-compose.yml; override them for other environments. */
export const appSettings = {
  apiBaseUrl: 'http://localhost:8080/api',
  keycloak: {
    url: 'http://localhost:8081',
    realm: 'rights',
    clientId: 'rights-web',
  },
};
