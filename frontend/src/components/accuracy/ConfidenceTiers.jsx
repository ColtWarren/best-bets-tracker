import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell, ReferenceLine } from 'recharts';

/**
 * Bar chart showing win rate by AI confidence tier.
 * The key insight: do higher-confidence picks actually hit more often?
 * A 50% reference line shows break-even.
 */
export default function ConfidenceTiers({ data }) {
  if (!data || data.length === 0) {
    return (
      <div className="chart-container empty-state">
        <h3>No Confidence Data Yet</h3>
      </div>
    );
  }

  return (
    <div className="chart-container">
      <h3>Win Rate by Confidence Tier</h3>
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={data} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
          <XAxis
            dataKey="tier"
            tick={{ fill: '#94a3b8', fontSize: 11 }}
            axisLine={{ stroke: 'rgba(124, 58, 237, 0.2)' }}
          />
          <YAxis
            domain={[0, 100]}
            tick={{ fill: '#94a3b8', fontSize: 12 }}
            axisLine={{ stroke: 'rgba(124, 58, 237, 0.2)' }}
            tickFormatter={(v) => `${v}%`}
          />
          <ReferenceLine
            y={50}
            stroke="#64748b"
            strokeDasharray="3 3"
            label={{ value: 'Break Even', fill: '#64748b', fontSize: 11 }}
          />
          <Tooltip
            contentStyle={{
              background: '#161b40',
              border: '1px solid rgba(124, 58, 237, 0.3)',
              borderRadius: 8,
              color: '#e2e8f0',
            }}
            formatter={(value, name, props) => [
              `${value.toFixed(1)}% (${props.payload.settled} bets)`,
              'Win Rate',
            ]}
          />
          <Bar dataKey="winRate" radius={[4, 4, 0, 0]}>
            {data.map((entry, index) => (
              <Cell
                key={index}
                fill={entry.winRate >= 55 ? '#7c3aed' : entry.winRate >= 50 ? '#6366f1' : '#ef4444'}
              />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
