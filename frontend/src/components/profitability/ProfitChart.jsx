import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, ReferenceLine } from 'recharts';

/**
 * Line chart showing cumulative profit/loss over time across daily snapshots.
 * Green line when profitable, red when in the negative.
 * Zero reference line shows break-even.
 */
export default function ProfitChart({ snapshots }) {
  if (!snapshots || snapshots.length === 0) {
    return (
      <div className="chart-container empty-state">
        <h3>No Profit Data Yet</h3>
      </div>
    );
  }

  // Build cumulative profit data from daily snapshots
  let cumulative = 0;
  const chartData = snapshots
    .sort((a, b) => a.snapshotDate.localeCompare(b.snapshotDate))
    .map((snap) => {
      cumulative += Number(snap.netProfitUnits) || 0;
      return {
        date: snap.snapshotDate,
        profit: Number(cumulative.toFixed(2)),
        dailyPL: Number(snap.netProfitUnits) || 0,
        record: `${snap.wins}-${snap.losses}`,
      };
    });

  const isProfitable = cumulative >= 0;

  return (
    <div className="chart-container">
      <h3>Cumulative Profit (Units)</h3>
      <ResponsiveContainer width="100%" height={350}>
        <LineChart data={chartData} margin={{ top: 10, right: 20, bottom: 5, left: 0 }}>
          <XAxis
            dataKey="date"
            tick={{ fill: '#94a3b8', fontSize: 11 }}
            axisLine={{ stroke: 'rgba(124, 58, 237, 0.2)' }}
            tickFormatter={(val) => {
              const d = new Date(val + 'T00:00:00');
              return `${d.getMonth() + 1}/${d.getDate()}`;
            }}
          />
          <YAxis
            tick={{ fill: '#94a3b8', fontSize: 12 }}
            axisLine={{ stroke: 'rgba(124, 58, 237, 0.2)' }}
            tickFormatter={(v) => `${v > 0 ? '+' : ''}${v}`}
          />
          <ReferenceLine y={0} stroke="#64748b" strokeDasharray="3 3" />
          <Tooltip
            contentStyle={{
              background: '#161b40',
              border: '1px solid rgba(124, 58, 237, 0.3)',
              borderRadius: 8,
              color: '#e2e8f0',
            }}
            formatter={(value, name) => {
              if (name === 'profit') return [`${value > 0 ? '+' : ''}${value} units`, 'Cumulative'];
              return [value, name];
            }}
            labelFormatter={(label) => `Date: ${label}`}
          />
          <Line
            type="monotone"
            dataKey="profit"
            stroke={isProfitable ? '#10b981' : '#ef4444'}
            strokeWidth={2}
            dot={false}
            activeDot={{ r: 5, fill: isProfitable ? '#10b981' : '#ef4444' }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
