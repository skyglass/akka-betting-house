import Link from 'next/link';
import withAuth from '../../../auth/middleware/withAuth';
import buildClient from "../../../api/build-client";
import { useKeycloak } from '../../../auth/provider/KeycloakProvider';
import { useEffect, useState } from "react";
import { useRouter } from "next/router";

const BetListPage = () => {
    const router = useRouter();
    const [bets, setBets] = useState([]);
    const { user } = useKeycloak();
    const { eventId } = router.query;

    useEffect(() => {
        if (user) {
            const fetchData = async () => {
                const client = buildClient({ req: {}, currentUser: user });
                const { data } = await client.get(`/api/betting/get-bets-for-market/${eventId}`);
                setBets(data.betDataList);
            };
            fetchData();
        }
    }, [user, eventId]);

    const getResultText = (result) => {
        switch (result) {
            case 0:
                return "Home Win";
            case 1:
                return "Away Win";
            case 2:
                return "Tie";
            default:
                return "Unknown";
        }
    };

    const betList = bets.map((bet) => {
        return (
            <tr key={bet.betId}>
                {/* Event Name */}
                <td>{bet.marketName}</td>

                {/* User */}
                <td>{bet.walletId}</td>

                {/* Result */}
                <td>{getResultText(bet.result)}</td>

                {/* Link to view bet details */}
                <td>
                    <Link href={`/bets/${bet.betId}`}>
                        View Bet
                    </Link>
                </td>
            </tr>
        );
    });

    return (
        <div>
            <h1>List of Bets for Event {eventId}</h1>
            <Link href="/bets/place">
                <button className="btn btn-primary" style={{ marginBottom: '10px' }}>Place a Bet</button>
            </Link>
            <table className="table">
                <thead>
                <tr>
                    <th>Event Name</th>
                    <th>User</th>
                    <th>Result</th>
                    <th>Action</th>
                </tr>
                </thead>
                <tbody>{betList}</tbody>
            </table>
        </div>
    );
};

export default withAuth(BetListPage);