import 'bootstrap/dist/css/bootstrap.css';
import buildClient from '../api/build-client';
import Header from '../components/header';
import { KeycloakProvider } from '../auth/provider/KeycloakProvider';
import { getCurrentUser } from '../auth/components/Helpers';

const AppComponent = ({ Component, pageProps, currentUser }) => {
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
  const currentUser = getCurrentUser();

  let pageProps = {};
  if (appContext.Component.getInitialProps) {
    pageProps = await appContext.Component.getInitialProps(
      appContext.ctx,
      client,
      currentUser
    );
  }

  return {
    pageProps,
    ...data,
  };
};

export default AppComponent;
