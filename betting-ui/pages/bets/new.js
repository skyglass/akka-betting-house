const BetIndex = ({ bets }) => {
  return (
    <ul>
      {bets.map((bet) => {
        return (
          <li key={bet.id}>
            {bet.ticket.title} - {bet.status}
          </li>
        );
      })}
    </ul>
  );
};

BetIndex.getInitialProps = async (context, client) => {
  const { data } = await client.get('/api/betting');

  return { bets: data };
};

export default BetIndex;
