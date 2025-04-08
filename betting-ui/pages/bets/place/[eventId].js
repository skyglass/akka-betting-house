import { useRouter } from "next/router";
import { useState, useEffect } from "react";
import {useKeycloak} from "../../../auth/provider/KeycloakProvider";
import buildClient from "../../../api/build-client";
import { v4 as uuidv4 } from 'uuid';

const NewBet = () => {
  const router = useRouter();
  const { eventId } = router.query;
  const { user } = useKeycloak();

  const [event, setEvent] = useState(null);
  const [selectedResult, setSelectedResult] = useState(0);
  const [stake, setStake] = useState("");

  useEffect(() => {
    if (!eventId) return;

    const fetchEvent = async () => {
      try {
        const client = buildClient({ req: {}, currentUser: user });
        const response = await client.get(`/api/market/get-state/${eventId}`);
        setEvent(response.data);
      } catch (error) {
        console.error("Error fetching event:", error);
      }
    };

    fetchEvent();
  }, [eventId]);

  if (!event) return <p>Loading event details...</p>;

  // Map result to corresponding odds
  const resultOptions = [
    { value: 0, label: "Home Win", odds: event.odds.winHome },
    { value: 1, label: "Away Win", odds: event.odds.winAway },
    { value: 2, label: "Tie", odds: event.odds.tie },
  ];

  const selectedOdds = resultOptions.find((opt) => opt.value === selectedResult)?.odds || 0;

  const handleStakeChange = (e) => {
    const value = e.target.value;
    if (/^\d*$/.test(value) && Number(value) > 0) {
      setStake(value);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const betData = {
      betId: uuidv4(),
      marketId: eventId,
      marketName: `${event.fixture.homeTeam} vs ${event.fixture.awayTeam}`,
      walletId: user.name,
      result: selectedResult,
      stake: Number(stake),
    };

    try {
      const client = buildClient({ req: {}, currentUser: user });
      const response = await client.post('/api/betting/open', betData);

      if (response.status !== 201) {
        const errorMessage = await response.data.message; // Get error message from server
        console.log("Failed to place bet:", errorMessage);
        return;
      }

      router.push("/bets");
    } catch (error) {
      console.error("Bet submission failed:", error);
    }
  };

  return (
      <div className="container mt-5 p-4 bg-white shadow rounded">
        <h2 className="h2 font-weight-bold mb-4">Place a Bet</h2>

        <form onSubmit={handleSubmit}>
          {/* Event Name (readonly) */}
          <div className="mb-4">
            <label className="form-label font-weight-semibold">Event Name</label>
            <input
                type="text"
                value={`${event.fixture.homeTeam} vs ${event.fixture.awayTeam}`}
                readOnly
                className="form-control bg-light"
            />
          </div>

          {/* Result Selection */}
          <div className="mb-4">
            <label className="form-label font-weight-semibold">Result</label>
            <select
                className="form-select"
                value={selectedResult}
                onChange={(e) => setSelectedResult(Number(e.target.value))}
            >
              {resultOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
              ))}
            </select>
          </div>

          {/* Odds (readonly) */}
          <div className="mb-4">
            <label className="form-label font-weight-semibold">Odds</label>
            <input
                type="text"
                value={selectedOdds}
                readOnly
                className="form-control bg-light"
            />
          </div>

          {/* Stake */}
          <div className="mb-4">
            <label className="form-label font-weight-semibold">Stake</label>
            <input
                type="text"
                value={stake}
                onChange={handleStakeChange}
                className="form-control"
                placeholder=" Enter stake (must be > 0)"
            />
          </div>

          {/* Submit Button */}
          <button
              type="submit"
              className="btn btn-primary w-100"
          >
            Place Bet
          </button>
        </form>
      </div>
  );
};

export default NewBet;