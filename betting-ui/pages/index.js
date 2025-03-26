import Link from 'next/link';
import withAuth from '../auth/middleware/withAuth';

const LandingPage = ({ currentUser, events }) => {
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

LandingPage.getInitialProps = async (context, client, currentUser) => {
  const { data } = await client.get('/api/market');

  return { events: data };
};

export default withAuth(LandingPage);
