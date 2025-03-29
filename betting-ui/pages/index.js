import Link from 'next/link';
import withAuth from '../auth/middleware/withAuth';
import buildClient from "../api/build-client";
import { useKeycloak } from '../auth/provider/KeycloakProvider';
import {useEffect, useState} from "react";

const LandingPage = () => {
    const [events, setEvents] = useState([]);
    const { user } = useKeycloak();

    useEffect(() => {
        if (user) {
            const fetchData = async () => {
                const client = buildClient({ req: {}, currentUser: user });  // Pass empty object for client-side
                const { data } = await client.get('/api/market/all');
                setEvents(data);
            };
            fetchData();
        }
    }, [user]);

  const eventList = events.map((event) => {
    return (
      <tr key={event.id}>
        <td>{event.title}</td>
        <td>{event.price}</td>
        <td>
          <Link href="/betting-ui/pages/events/[eventId]" as={`/events/${event.id}`}>
            View
          </Link>
        </td>
      </tr>
    );
  });

  return (
      <div>
          <h1>Events</h1>
          <table className="table">
              <thead>
              <tr>
                  <th>Title</th>
                  <th>Price</th>
                  <th>Link</th>
              </tr>
              </thead>
              <tbody>{eventList}</tbody>
          </table>
      </div>
  );
};

export default withAuth(LandingPage);
