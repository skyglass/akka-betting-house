import { useEffect, useState } from 'react';
import Router from 'next/router';
import { useKeycloak } from '../../auth/provider/KeycloakProvider';
import buildClient from '../../api/build-client';
import { useRouter } from 'next/router';

const EventShow = () => {
    const { user } = useKeycloak();
    const router = useRouter();
    const { eventId } = router.query;
    const [event, setEvent] = useState(null);
    const [errors, setErrors] = useState(null);

    useEffect(() => {
        if (user && eventId) {
            const fetchEvent = async () => {
                try {
                    const client = buildClient({ req: {}, currentUser: user });
                    const { data } = await client.get(`/api/market/get-state/${eventId}`);
                    setEvent(data);
                } catch (err) {
                    setErrors(err.response?.data?.errors || [{ message: "Failed to fetch event" }]);
                }
            };
            fetchEvent();
        }
    }, [user, eventId]);

    const placeBet = async () => {
        setErrors(null);
        try {
            const client = buildClient({ req: {}, currentUser: user });
            const { data: bet } = await client.post('/api/betting', { marketId: event.id });
            Router.push('/bets/[betId]', `/bets/${bet.id}`);
        } catch (err) {
            setErrors(err.response?.data?.errors || [{ message: "Failed to place bet" }]);
        }
    };

    const getResultText = (open, result) => {
        if (open) {
            return "OPEN";
        }
        return result;
    };

    if (!event) {
        return <div>Loading...</div>;
    }

    return (
        <div>
            <h1>{event.fixture.homeTeam} vs {event.fixture.awayTeam}</h1>

            {errors && (
                <div className="alert alert-danger">
                    <ul>
                        {errors.map((err, index) => (
                            <li key={index}>{err.message}</li>
                        ))}
                    </ul>
                </div>
            )}

            <h3>Fixture Information</h3>
            <div className="fixture-data">
                {event.fixture ? (
                    <div>
                        <p><strong>Fixture ID:</strong> {event.fixture.id}</p>
                        <p><strong>Home Team:</strong> {event.fixture.homeTeam}</p>
                        <p><strong>Away Team:</strong> {event.fixture.awayTeam}</p>
                    </div>
                ) : (
                    <p>No fixture data available.</p>
                )}
            </div>

            <h3>Odds Information</h3>
            <div className="odds-data">
                {event.odds ? (
                    <div>
                        <p><strong>Win Home:</strong> {event.odds.winHome}</p>
                        <p><strong>Win Away:</strong> {event.odds.winAway}</p>
                        <p><strong>Tie:</strong> {event.odds.tie}</p>
                    </div>
                ) : (
                    <p>No odds data available.</p>
                )}
            </div>

            <h3>Additional Information</h3>
            <div className="additional-info">
                {event.opensAt && (
                    <p><strong>Opened At:</strong> {new Date(event.opensAt).toLocaleString()}</p>
                )}
                {/* Add the Result field */}
                <p><strong>Result:</strong> {getResultText(event.open, event.result)}</p>
            </div>

            <button onClick={placeBet} className="btn btn-primary">
                Place Bet
            </button>
        </div>
    );
};

export default EventShow;