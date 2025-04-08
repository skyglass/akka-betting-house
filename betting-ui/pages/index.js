import Link from 'next/link';
import withAuth from '../auth/middleware/withAuth';
import buildClient from "../api/build-client";
import { useKeycloak } from '../auth/provider/KeycloakProvider';
import { useEffect, useState } from "react";

const LandingPage = () => {
    const [events, setEvents] = useState([]);
    const { user } = useKeycloak();

    useEffect(() => {
        if (user) {
            const fetchData = async () => {
                const client = buildClient({ req: {}, currentUser: user });
                const { data } = await client.get('/api/market/all');
                setEvents(data);
            };
            fetchData();
        }
    }, [user]);

    const getResultText = (open, result) => {
        if (open) {
            return "OPEN";
        }
        return result;
    };

    const eventList = events.map((event) => {
        return (
            <tr key={event.marketId}>
                {/* Market Fixture */}
                <td>{event.fixture.homeTeam} vs {event.fixture.awayTeam}</td>

                {/* Odds (Grouped) */}
                <td>
                    <table>
                        <tbody>
                        <tr>
                            <td><strong>Home Win:</strong> {event.odds.winHome}</td>
                        </tr>
                        <tr>
                            <td><strong>Away Win:</strong> {event.odds.winAway}</td>
                        </tr>
                        <tr>
                            <td><strong>Tie:</strong> {event.odds.tie}</td>
                        </tr>
                        </tbody>
                    </table>
                </td>

                {/* Result */}
                <td>{getResultText(event.open, event.result)}</td>

                {/* Link to view details */}
                <td>
                    <Link href={`/events/${event.marketId}`}>
                        View
                    </Link>
                </td>

                <td>
                    <Link href={`/bets/place/${event.marketId}`}>
                        Place Bet
                    </Link>
                </td>

                <td>
                    <Link href={`/bets/view/${event.marketId}`}>
                        View Bets
                    </Link>
                </td>
            </tr>
        );
    });

    return (
        <div>
            <h1>Events</h1>
            <Link href="/events/new">
                <button className="btn btn-primary" style={{ marginBottom: '10px' }}>Add Event</button>
            </Link>
            <table className="table">
                <thead>
                <tr>
                    <th>Fixture</th>
                    <th>Odds</th>
                    <th>Result</th>
                    <th>Link</th>
                    <th>Action</th>
                    <th>View Bets</th>
                </tr>
                </thead>
                <tbody>{eventList}</tbody>
            </table>
        </div>
    );
};

export default withAuth(LandingPage);