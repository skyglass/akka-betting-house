import Router from 'next/router';
import useRequest from '../../hooks/use-request';

const CustomerShow = ({ customer }) => {
  const { doRequest, errors } = useRequest({
    url: '/api/betting',
    method: 'post',
    body: {
      customerId: customer.id,
    },
    onSuccess: (bet) =>
        Router.push('/bets/[betId]', `/bets/${bet.id}`),
  });

  return (
      <div>
        <h1>{customer.title}</h1>
        <h4>Balance: {customer.balance}</h4>
        {errors}
        <button onClick={() => doRequest()} className="btn btn-primary">
          Place Bet
        </button>
      </div>
  );
};

CustomerShow.getInitialProps = async (context, client) => {
  const { customerId } = context.query;
  const { data } = await client.get(`/api/customer/${customerId}`);

  return { customer: data };
};

export default CustomerShow;
