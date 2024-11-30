import React, { createContext, useContext, useEffect, useState } from 'react';
import { initKeycloak, keycloak, logout } from '../config/keycloak';

const KeycloakContext = createContext({
  initialized: false,
  authenticated: false,
  user: null,
  logout: () => {},
});

export const KeycloakProvider = ({ children }) => {
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [user, setUser] = useState(null);

  useEffect(() => {
    if (typeof window !== 'undefined') {
      initKeycloak()
        .then(data => {
          setAuthenticated(data.authenticated);
          setUser(data.user);
          setInitialized(true);
        })
        .catch(err => console.error('Failed to initialize Keycloak', err));
    }
  }, []);

  return (
    <KeycloakContext.Provider value={{ initialized, authenticated, user, logout }}>
      {children}
    </KeycloakContext.Provider>
  );
};

export const useKeycloak = () => useContext(KeycloakContext);