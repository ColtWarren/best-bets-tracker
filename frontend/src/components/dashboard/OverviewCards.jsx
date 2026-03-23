/**
 * Top-level stat cards showing headline numbers:
 * win rate, ROI, net units, total picks, CLV rate, streak.
 */
export default function OverviewCards({ stats, streak }) {
  if (!stats) return null;

  const cards = [
    {
      label: 'Win Rate',
      value: `${stats.winRate?.toFixed(1) || 0}%`,
      className: stats.winRate > 52 ? 'positive' : stats.winRate < 48 ? 'negative' : 'neutral',
    },
    {
      label: 'ROI',
      value: `${Number(stats.roi) >= 0 ? '+' : ''}${Number(stats.roi)?.toFixed(1) || 0}%`,
      className: Number(stats.roi) > 0 ? 'positive' : Number(stats.roi) < 0 ? 'negative' : 'neutral',
    },
    {
      label: 'Net Units',
      value: `${Number(stats.netUnits) >= 0 ? '+' : ''}${Number(stats.netUnits)?.toFixed(2) || '0.00'}`,
      className: Number(stats.netUnits) > 0 ? 'positive' : Number(stats.netUnits) < 0 ? 'negative' : 'neutral',
    },
    {
      label: 'Record',
      value: `${stats.wins || 0}-${stats.losses || 0}`,
      className: 'neutral',
    },
    {
      label: 'Total Picks',
      value: `${stats.totalPicks || 0}`,
      className: 'neutral',
    },
    {
      label: 'Pending',
      value: `${stats.pending || 0}`,
      className: 'neutral',
    },
    {
      label: 'CLV Rate',
      value: `${Number(stats.clvRate)?.toFixed(1) || 0}%`,
      className: Number(stats.clvRate) > 55 ? 'positive' : 'neutral',
    },
    {
      label: 'Streak',
      value: streak
        ? `${streak.count} ${streak.type === 'WIN_STREAK' ? 'W' : streak.type === 'LOSS_STREAK' ? 'L' : '-'}`
        : '-',
      className: streak?.type === 'WIN_STREAK' ? 'positive' : streak?.type === 'LOSS_STREAK' ? 'negative' : 'neutral',
    },
  ];

  return (
    <div className="stat-grid">
      {cards.map(({ label, value, className }) => (
        <div className="stat-card" key={label}>
          <div className="stat-label">{label}</div>
          <div className={`stat-value ${className}`}>{value}</div>
        </div>
      ))}
    </div>
  );
}
