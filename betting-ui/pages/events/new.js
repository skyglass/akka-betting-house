import {useState} from 'react';
import Router from 'next/router';
import { useKeycloak } from "../../auth/provider/KeycloakProvider";
import buildClient from "../../api/build-client";
import { v4 as uuidv4 } from 'uuid';

const NewEvent = () => {
  const { user } = useKeycloak();
  const [homeTeam, setHomeTeam] = useState('');
  const [awayTeam, setAwayTeam] = useState('');
  const [winHome, setWinHome] = useState('');
  const [winAway, setWinAway] = useState('');
  const [tie, setTie] = useState('');
  const [errors, setErrors] = useState(null);

  const onSubmit = async (event) => {
    event.preventDefault();
    setErrors(null);

    try {
      const client = buildClient({ req: {}, currentUser: user });
      const fixtureData = {
        id: uuidv4(), // Unique fixture ID
        homeTeam,
        awayTeam,
      };
      const oddsData = {
        winHome: parseFloat(winHome),
        winAway: parseFloat(winAway),
        tie: parseFloat(tie),
      };

      const data = {
        marketId: uuidv4(), // Unique market ID
        fixture: fixtureData,
        odds: oddsData,
        opensAt: Date.now(), // Example timestamp; adjust as needed
      };

      await client.post('/api/market/open', data);
      Router.push('/');
    } catch (err) {
      setErrors(err.response?.data?.errors || [{ message: "An error occurred" }]);
    }
  };

  return (
      <div>
        <h1>Create Event</h1>
        <form onSubmit={onSubmit}>
          {/* Fixture Data Group */}
          <div className="form-group">
            <h3>Fixture Details</h3>
            <label>Home Team</label>
            <input
                value={homeTeam}
                onChange={(e) => setHomeTeam(e.target.value)}
                className="form-control"
            />
          </div>
          <div className="form-group">
            <label>Away Team</label>
            <input
                value={awayTeam}
                onChange={(e) => setAwayTeam(e.target.value)}
                className="form-control"
            />
          </div>

          {/* Odds Data Group */}
          <div className="form-group">
            <h3>Odds</h3>
            <label>Win Home Odds</label>
            <input
                value={winHome}
                onChange={(e) => setWinHome(e.target.value)}
                className="form-control"
            />
          </div>
          <div className="form-group">
            <label>Win Away Odds</label>
            <input
                value={winAway}
                onChange={(e) => setWinAway(e.target.value)}
                className="form-control"
            />
          </div>
          <div className="form-group">
            <label>Tie Odds</label>
            <input
                value={tie}
                onChange={(e) => setTie(e.target.value)}
                className="form-control"
            />
          </div>

          {errors && (
              <div className="alert alert-danger">
                <ul>
                  {errors.map((err, index) => (
                      <li key={index}>{err.message}</li>
                  ))}
                </ul>
              </div>
          )}

          <button className="btn btn-primary">Submit</button>
        </form>
      </div>
  );
};

export default NewEvent;