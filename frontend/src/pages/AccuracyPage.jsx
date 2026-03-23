import { useState, useEffect } from 'react';
import { getAccuracyBySport, getAccuracyByBetType, getAccuracyByConfidence, getOverallAccuracy } from '../api/api';
import SportBreakdown from '../components/accuracy/SportBreakdown';
import ConfidenceTiers from '../components/accuracy/ConfidenceTiers';

/**
 * Accuracy deep-dive page — answers "how accurate is the AI?"
 * Shows breakdowns by sport, bet type, and confidence tier.
 */
export default function AccuracyPage() {
  const [overall, setOverall] = useState(null);
  const [bySport, setBySport] = useState([]);
  const [byType, setByType] = useState([]);
  const [byConfidence, setByConfidence] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [overallRes, sportRes, typeRes, confRes] = await Promise.all([
        getOverallAccuracy(),
        getAccuracyBySport(),
        getAccuracyByBetType(),
        getAccuracyByConfidence(),
      ]);
      setOverall(overallRes.data);
      setBySport(sportRes.data);
      setByType(typeRes.data);
      setByConfidence(confRes.data);
    } catch (err) {
      console.error('Failed to load accuracy data:', err);
    }
    setLoading(false);
  };

  if (loading) return <div className="loading">Loading accuracy data</div>;

  return (
    <div>
      <div className="page-header">
        <h1>Accuracy Analysis</h1>
        <p>How reliable are the AI recommendations?</p>
      </div>

      {/* Overall headline */}
      {overall && (
        <div className="stat-grid" style={{ marginBottom: 32 }}>
          <div className="stat-card">
            <div className="stat-label">Overall Win Rate</div>
            <div className={`stat-value ${overall.winRate > 52 ? 'positive' : 'neutral'}`}>
              {overall.winRate?.toFixed(1)}%
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-label">Avg Confidence</div>
            <div className="stat-value neutral">
              {overall.avgConfidence?.toFixed(1)}/10
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-label">Avg Odds</div>
            <div className="stat-value neutral" style={{ fontSize: '1.3rem' }}>
              {overall.avgOdds > 0 ? '+' : ''}{overall.avgOdds?.toFixed(0)}
            </div>
          </div>
        </div>
      )}

      <div className="grid-2">
        <SportBreakdown data={bySport} />
        <ConfidenceTiers data={byConfidence} />
      </div>

      {/* Bet Type Table */}
      {byType.length > 0 && (
        <div className="card section">
          <h3 className="section-title">Win Rate by Bet Type</h3>
          <table className="data-table">
            <thead>
              <tr>
                <th>Bet Type</th>
                <th>Win Rate</th>
              </tr>
            </thead>
            <tbody>
              {byType.map((t) => (
                <tr key={t.betType}>
                  <td><span className="badge badge-sport">{t.betType}</span></td>
                  <td>
                    <span className={`stat-value ${t.winRate > 52 ? 'positive' : t.winRate < 48 ? 'negative' : 'neutral'}`}
                          style={{ fontSize: '0.95rem' }}>
                      {t.winRate?.toFixed(1)}%
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
