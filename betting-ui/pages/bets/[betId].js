import { useEffect, useState } from 'react';
import StripeCheckout from 'react-stripe-checkout';
import Router from 'next/router';
import useRequest from '../../hooks/use-request';

const BetShow = ({ bet, currentUser }) => {
  const [timeLeft, setTimeLeft] = useState(0);
  const { doRequest, errors } = useRequest({
    url: '/api/betting',
    method: 'post',
    body: {
      betId: bet.id,
    },
    onSuccess: () => Router.push('/bets'),
  });

  useEffect(() => {
    const findTimeLeft = () => {
      const msLeft = new Date(bet.expiresAt) - new Date();
      setTimeLeft(Math.round(msLeft / 1000));
    };

    findTimeLeft();
    const timerId = setInterval(findTimeLeft, 1000);

    return () => {
      clearInterval(timerId);
    };
  }, [bet]);

  if (timeLeft < 0) {
    return <div>Bet Expired</div>;
  }

  return (
    <div>
      Time left to pay: {timeLeft} seconds
      {errors}
    </div>
  );
};

BetShow.getInitialProps = async (context, client) => {
  const { betId } = context.query;
  const { data } = await client.get(`/api/betting/${betId}`);

  return { bet: data };
};

export default BetShow;
