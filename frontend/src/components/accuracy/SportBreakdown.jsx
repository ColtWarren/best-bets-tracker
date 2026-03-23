import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';

/**
 * Bar chart showing win rate by sport.
 * Bars are colored green (profitable) or red (losing) based on the 50% threshold.
 */
export default function SportBreakdown({ data }) {
  if (!data || data.length === 0) {
    return (
      <div className="chart-container empty-state">
        <h3>No Sport Data Yet</h3>
      </div>
    );
  }

  return (
    <div className="chart-container">
      <h3>Win Rate by Sport</h3>
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={data} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
          <XAxis
            dataKey="sport"
            tick={{ fill: '#94a3b8', fontSize: 12 }}
            axisLine={{ stroke: 'rgba(124, 58, 237, 0.2)' }}
          />
          <YAxis
            domain={[0, 100]}
            tick={{ fill: '#94a3b8', fontSize: 12 }}
            axisLine={{ stroke: 'rgba(124, 58, 237, 0.2)' }}
            tickFormatter={(v) => `${v}%`}
          />
          <Tooltip
            contentStyle={{
              background: '#161b40',
              border: '1px solid rgba(124, 58, 237, 0.3)',
              borderRadius: 8,
              color: '#e2e8f0',
            }}
            formatter={(value) => [`${value.toFixed(1)}%`, 'Win Rate']}
          />
          <Bar dataKey="winRate" radius={[4, 4, 0, 0]}>
            {data.map((entry, index) => (
              <Cell
                key={index}
                fill={entry.winRate >= 52 ? '#10b981' : entry.winRate >= 48 ? '#f59e0b' : '#ef4444'}
              />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
