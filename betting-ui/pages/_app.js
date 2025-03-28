import 'bootstrap/dist/css/bootstrap.css';
import buildClient from '../api/build-client';
import Header from '../components/header';
import { KeycloakProvider } from '../auth/provider/KeycloakProvider';
import { initKeycloak } from '../auth/config/keycloak';
import { useKeycloak } from '../auth/provider/KeycloakProvider';

const AppComponent = ({ Component, pageProps }) => {
  const { currentUser } = useKeycloak();
  return (
    <KeycloakProvider>
      <Header currentUser={currentUser} />
      <div className="container">
        <Component currentUser={currentUser} {...pageProps} />
      </div>
    </KeycloakProvider>
  );
};

AppComponent.getInitialProps = async (appContext) => {
  const client = buildClient(appContext.ctx);
  let currentUser = null;
  if (typeof window === 'undefined') {
    // Fetch the current user on the server
    try {
      const keycloakData = await initKeycloak();
      currentUser = keycloakData.user;
    } catch (err) {
      console.error('Keycloak initialization failed on server', err);
    }
  }

  const data = {
    currentUser: currentUser
  }

  let pageProps = {};
  if (appContext.Component.getInitialProps) {
    pageProps = await appContext.Component.getInitialProps(appContext.ctx, client, currentUser);
  }

  return {
    pageProps,
    ...data,
  };
};

export default AppComponent;