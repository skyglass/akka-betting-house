import Link from 'next/link';
import LogoutButton from '../auth/components/LogoutButton';
import {useKeycloak} from "../auth/provider/KeycloakProvider";
import {isAdminFunc} from "../auth/components/Helpers";

export default ({  }) => {
  const { user } = useKeycloak();
  const links = [
    user && { label: 'My Bets', href: '/bets' },
    user && isAdminFunc(user) && { label: 'Players', href: '/customers' },
  ]
    .filter((linkConfig) => linkConfig)
    .map(({ label, href }) => {
      return (
        <li key={href} className="nav-item">
          <Link className="nav-link" href={href}>
            {label}
          </Link>
        </li>
      );
    });

  return (
    <nav className="navbar navbar-light bg-light">
      <Link className="navbar-brand" href="/">
        SkyComposer
      </Link>

      <div className="d-flex justify-content-end">
        <ul className="nav d-flex align-items-center">{links}</ul>
      </div>

      <div>
        <LogoutButton />
      </div>
    </nav>
  );
};
