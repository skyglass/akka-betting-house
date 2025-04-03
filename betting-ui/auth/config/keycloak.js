import Keycloak from 'keycloak-js';
import { config } from '../../Constants'

const keycloakConfig = {
  url: config.keycloak.BASE_URL, //process.env.KEYCLOAK_BASE_URL, // Keycloak URL
  realm: config.keycloak.REALM, // Keycloak Realm
  clientId: config.keycloak.CLIENT_ID // Keycloak OAUTH2 client ID
};

let keycloak;

if (typeof window !== 'undefined') {
  keycloak = new Keycloak(keycloakConfig);
}

let isInitialized = false;

export const initKeycloak = async () => {
  if (!isInitialized && keycloak) {
    isInitialized = true;
    try {
      const authenticated = await keycloak.init({ onLoad: 'login-required', checkLoginIframe: false });
      if (authenticated) {
        return {
          authenticated: true,
          user: {
            name: keycloak.tokenParsed?.preferred_username,
            email: keycloak.tokenParsed?.email,
            token: keycloak.token,
            tokenParsed: keycloak.tokenParsed
          },
        };
      }
    } catch (err) {
      isInitialized = false;
      console.error('Failed to initialize Keycloak', err);
      throw err;
    }
  }
  return { authenticated: false, user: null };
};

export const logout = () => {
  if (keycloak) {
    keycloak.logout();
  }
};

export const getToken = async () => {
  if (keycloak) {
    if (keycloak.isTokenExpired()) {
      try {
        await keycloak.updateToken(30);
      } catch (error) {
        console.error('Failed to refresh the token', error);
        keycloak.logout();
        return null;
      }
    }
    return keycloak.token ?? null;
  }
  return null;
};

export { keycloak };