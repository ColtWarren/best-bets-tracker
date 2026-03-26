import { useState, useEffect } from 'react';
import { getPredictions } from '../api/api';

/**
 * Predictions page — browse and filter all captured AI recommendations.
 * Filterable by sport and status.
 */
export default function PredictionsPage() {
  const [predictions, setPredictions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sportFilter, setSportFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  useEffect(() => {
    loadPredictions();
  }, [sportFilter, statusFilter]);

  const loadPredictions = async () => {
    setLoading(true);
    try {
      const params = {};
      if (sportFilter) params.sport = sportFilter;
      if (statusFilter) params.status = statusFilter;
      const { data } = await getPredictions(params);
      setPredictions(data);
    } catch (err) {
      setPredictions([]);
    }
    setLoading(false);
  };

  const sports = ['', 'NFL', 'NBA', 'MLB', 'NHL', 'CFB', 'CBB', 'WCBB', 'WNBA', 'SOCCER'];
  const statuses = ['', 'PENDING', 'WON', 'LOST', 'PUSH'];

  return (
    <div>
      <div className="page-header">
        <h1>Predictions</h1>
        <p>All AI best bet recommendations</p>
      </div>

      <div style={{ display: 'flex', gap: 12, marginBottom: 24 }}>
        <select
          value={sportFilter}
          onChange={(e) => setSportFilter(e.target.value)}
          style={styles.select}
        >
          <option value="">All Sports</option>
          {sports.filter(Boolean).map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>

        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          style={styles.select}
        >
          <option value="">All Statuses</option>
          {statuses.filter(Boolean).map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>

        <span style={{ color: 'var(--text-muted)', alignSelf: 'center', fontSize: '0.85rem' }}>
          {predictions.length} predictions
        </span>
      </div>

      {loading ? (
        <div className="loading">Loading predictions</div>
      ) : predictions.length === 0 ? (
        <div className="empty-state">
          <h3>No Predictions Found</h3>
          <p>Capture today's picks to get started.</p>
        </div>
      ) : (
        <div className="card">
          <table className="data-table">
            <thead>
              <tr>
                <th>Status</th>
                <th>Sport</th>
                <th>Matchup</th>
                <th>Pick</th>
                <th>Confidence</th>
                <th>Odds</th>
                <th>Kelly %</th>
                <th>Book</th>
              </tr>
            </thead>
            <tbody>
              {predictions.map((p) => (
                <tr key={p.id}>
                  <td>
                    <span className={`badge badge-${p.status?.toLowerCase()}`}>
                      {p.status}
                    </span>
                  </td>
                  <td><span className="badge badge-sport">{p.sport}</span></td>
                  <td>{p.awayTeam} @ {p.homeTeam}</td>
                  <td style={{ fontWeight: 600 }}>{p.selection}</td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>
                    {Number(p.confidence)?.toFixed(1)}/10
                  </td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>
                    {p.odds > 0 ? `+${p.odds}` : p.odds}
                  </td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>
                    {Number(p.kellyPercent)?.toFixed(1)}%
                  </td>
                  <td>{p.sportsbook || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

const styles = {
  select: {
    padding: '8px 12px',
    background: '#161b40',
    color: '#e2e8f0',
    border: '1px solid rgba(124, 58, 237, 0.3)',
    borderRadius: 8,
    fontSize: '0.85rem',
    fontFamily: 'inherit',
    cursor: 'pointer',
  },
};
