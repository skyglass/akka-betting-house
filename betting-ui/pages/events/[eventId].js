import Router from 'next/router';
import useRequest from '../../hooks/use-request';

const EventShow = ({ event }) => {
  const { doRequest, errors } = useRequest({
    url: '/api/betting',
    method: 'post',
    body: {
      marketId: event.id,
    },
    onSuccess: (bet) =>
      Router.push('/bets/[betId]', `/bets/${bet.id}`),
  });

  return (
    <div>
      <h1>{event.title}</h1>
      <h4>Price: {event.price}</h4>
      {errors}
      <button onClick={() => doRequest()} className="btn btn-primary">
        Place Bet
      </button>
    </div>
  );
};

EventShow.getInitialProps = async (context, client) => {
  const { eventId } = context.query;
  const { data } = await client.get(`/api/market/${eventId}`);

  return { event: data };
};

export default EventShow;
