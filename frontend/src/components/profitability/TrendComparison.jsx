/**
 * Shows win rate and profit across multiple time windows (7, 14, 30, 90 days).
 * Helps identify if the AI is improving or declining over time.
 */
export default function TrendComparison({ trends }) {
  if (!trends || trends.length === 0) return null;

  return (
    <div className="card">
      <h3 className="section-title">Performance Trends</h3>
      <table className="data-table">
        <thead>
          <tr>
            <th>Period</th>
            <th>Record</th>
            <th>Win Rate</th>
            <th>Net Units</th>
          </tr>
        </thead>
        <tbody>
          {trends.map((trend) => {
            const winRateClass = trend.winRate > 52 ? 'positive'
              : trend.winRate < 48 ? 'negative' : 'neutral';
            const unitsClass = Number(trend.netUnits) > 0 ? 'positive'
              : Number(trend.netUnits) < 0 ? 'negative' : 'neutral';

            return (
              <tr key={trend.period}>
                <td>{trend.period}</td>
                <td style={{ fontFamily: 'var(--font-mono)' }}>
                  {trend.wins}-{trend.losses}
                </td>
                <td>
                  <span className={`stat-value ${winRateClass}`} style={{ fontSize: '0.9rem' }}>
                    {trend.winRate?.toFixed(1)}%
                  </span>
                </td>
                <td style={{ fontFamily: 'var(--font-mono)' }}>
                  <span className={`stat-value ${unitsClass}`} style={{ fontSize: '0.9rem' }}>
                    {Number(trend.netUnits) > 0 ? '+' : ''}{Number(trend.netUnits)?.toFixed(2)}u
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
