/**
 * Table showing the most recently settled predictions with their outcomes.
 * Color-coded by result (WON = green, LOST = red, PUSH = gray).
 */
export default function RecentResults({ outcomes }) {
  if (!outcomes || outcomes.length === 0) {
    return (
      <div className="card empty-state">
        <h3>No Results Yet</h3>
        <p>Outcomes will appear here once predictions are settled.</p>
      </div>
    );
  }

  return (
    <div className="card">
      <h3 className="section-title">Recent Results</h3>
      <table className="data-table">
        <thead>
          <tr>
            <th>Result</th>
            <th>Matchup</th>
            <th>Pick</th>
            <th>Score</th>
            <th>Odds</th>
            <th>P/L</th>
          </tr>
        </thead>
        <tbody>
          {outcomes.map((outcome) => {
            const pred = outcome.prediction || {};
            const resultClass = `badge badge-${outcome.betResult?.toLowerCase()}`;
            const profitClass = Number(outcome.profitUnits) > 0
              ? 'positive' : Number(outcome.profitUnits) < 0
              ? 'negative' : 'neutral';

            return (
              <tr key={outcome.id}>
                <td>
                  <span className={resultClass}>{outcome.betResult}</span>
                </td>
                <td>
                  <span className="badge badge-sport">{pred.sport}</span>{' '}
                  {pred.eventName || 'Unknown'}
                </td>
                <td>{pred.selection || '-'}</td>
                <td style={{ fontFamily: 'var(--font-mono)' }}>
                  {outcome.finalScoreHome != null
                    ? `${outcome.finalScoreHome}-${outcome.finalScoreAway}`
                    : '-'}
                </td>
                <td style={{ fontFamily: 'var(--font-mono)' }}>
                  {pred.odds > 0 ? `+${pred.odds}` : pred.odds}
                </td>
                <td style={{ fontFamily: 'var(--font-mono)' }}>
                  <span className={`stat-value ${profitClass}`} style={{ fontSize: '0.9rem' }}>
                    {Number(outcome.profitUnits) > 0 ? '+' : ''}
                    {Number(outcome.profitUnits)?.toFixed(2) || '0.00'}u
                  </span>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
