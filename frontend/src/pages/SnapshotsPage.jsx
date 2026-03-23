import { useState, useEffect } from 'react';
import { getSnapshots } from '../api/api';

/**
 * Daily history page — shows each day's snapshot with win/loss record and profit.
 */
export default function SnapshotsPage() {
  const [snapshots, setSnapshots] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadSnapshots();
  }, []);

  const loadSnapshots = async () => {
    try {
      const { data } = await getSnapshots();
      setSnapshots(data);
    } catch (err) {
      console.error('Failed to load snapshots:', err);
    }
    setLoading(false);
  };

  if (loading) return <div className="loading">Loading daily history</div>;

  return (
    <div>
      <div className="page-header">
        <h1>Daily History</h1>
        <p>Day-by-day breakdown of AI predictions and outcomes</p>
      </div>

      {snapshots.length === 0 ? (
        <div className="empty-state">
          <h3>No Snapshots Yet</h3>
          <p>Capture your first day's picks to get started.</p>
        </div>
      ) : (
        <div className="card">
          <table className="data-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Picks</th>
                <th>Record</th>
                <th>Win Rate</th>
                <th>Net P/L</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {snapshots.map((snap) => {
                const resolved = snap.wins + snap.losses + snap.pushes;
                const isComplete = resolved >= snap.totalPicks && snap.totalPicks > 0;
                const profitClass = Number(snap.netProfitUnits) > 0 ? 'positive'
                  : Number(snap.netProfitUnits) < 0 ? 'negative' : 'neutral';

                return (
                  <tr key={snap.id}>
                    <td style={{ fontWeight: 600 }}>{snap.snapshotDate}</td>
                    <td style={{ fontFamily: 'var(--font-mono)' }}>{snap.totalPicks}</td>
                    <td style={{ fontFamily: 'var(--font-mono)' }}>
                      {snap.wins}-{snap.losses}{snap.pushes > 0 ? `-${snap.pushes}` : ''}
                    </td>
                    <td style={{ fontFamily: 'var(--font-mono)' }}>
                      {Number(snap.winRate)?.toFixed(1) || '0.0'}%
                    </td>
                    <td style={{ fontFamily: 'var(--font-mono)' }}>
                      <span className={`stat-value ${profitClass}`} style={{ fontSize: '0.9rem' }}>
                        {Number(snap.netProfitUnits) > 0 ? '+' : ''}
                        {Number(snap.netProfitUnits)?.toFixed(2) || '0.00'}u
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${isComplete ? 'badge-won' : 'badge-pending'}`}>
                        {isComplete ? 'Complete' : `${snap.totalPicks - resolved} pending`}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
