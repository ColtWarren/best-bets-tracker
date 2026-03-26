import { useState, useEffect } from 'react';
import { getSnapshots, getBankrollSummary, getTrendComparison, getProfitBySportsbook } from '../api/api';
import ProfitChart from '../components/profitability/ProfitChart';
import TrendComparison from '../components/profitability/TrendComparison';

/**
 * Profitability page — answers "would I have made money following these picks?"
 * Shows cumulative P/L chart, simulated bankroll, trends, and sportsbook breakdown.
 */
export default function ProfitabilityPage() {
  const [snapshots, setSnapshots] = useState([]);
  const [bankroll, setBankroll] = useState(null);
  const [trends, setTrends] = useState([]);
  const [sportsbookPL, setSportsbookPL] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [snapRes, bankRes, trendRes, bookRes] = await Promise.all([
        getSnapshots(),
        getBankrollSummary(1000),
        getTrendComparison(),
        getProfitBySportsbook(),
      ]);
      setSnapshots(snapRes.data);
      setBankroll(bankRes.data);
      setTrends(trendRes.data);
      setSportsbookPL(bookRes.data);
    } catch {
      // Error state handled by null/empty checks in render
    }
    setLoading(false);
  };

  if (loading) return <div className="loading">Loading profitability data</div>;

  return (
    <div>
      <div className="page-header">
        <h1>Profitability</h1>
        <p>Would following the AI picks make money?</p>
      </div>

      {/* Simulated Bankroll Cards */}
      {bankroll && (
        <div className="stat-grid">
          <div className="stat-card">
            <div className="stat-label">Starting Bankroll</div>
            <div className="stat-value neutral">
              ${Number(bankroll.startingBankroll)?.toLocaleString()}
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-label">Current Bankroll</div>
            <div className={`stat-value ${Number(bankroll.totalProfitLoss) >= 0 ? 'positive' : 'negative'}`}>
              ${Number(bankroll.currentBankroll)?.toLocaleString()}
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-label">Total P/L</div>
            <div className={`stat-value ${Number(bankroll.totalProfitLoss) >= 0 ? 'positive' : 'negative'}`}>
              {Number(bankroll.totalProfitLoss) >= 0 ? '+' : ''}
              ${Number(bankroll.totalProfitLoss)?.toFixed(2)}
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-label">ROI</div>
            <div className={`stat-value ${Number(bankroll.roi) >= 0 ? 'positive' : 'negative'}`}>
              {Number(bankroll.roi) >= 0 ? '+' : ''}{Number(bankroll.roi)?.toFixed(1)}%
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-label">Growth</div>
            <div className={`stat-value ${Number(bankroll.growth) >= 0 ? 'positive' : 'negative'}`}>
              {Number(bankroll.growth) >= 0 ? '+' : ''}{Number(bankroll.growth)?.toFixed(1)}%
            </div>
          </div>
        </div>
      )}

      <ProfitChart snapshots={snapshots} />

      <div className="grid-2">
        <TrendComparison trends={trends} />

        {/* Sportsbook P/L */}
        {sportsbookPL.length > 0 && (
          <div className="card">
            <h3 className="section-title">P/L by Sportsbook</h3>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Sportsbook</th>
                  <th>Bets</th>
                  <th>P/L</th>
                </tr>
              </thead>
              <tbody>
                {sportsbookPL.map((book) => (
                  <tr key={book.sportsbook}>
                    <td>{book.sportsbook}</td>
                    <td style={{ fontFamily: 'var(--font-mono)' }}>{book.betCount}</td>
                    <td style={{ fontFamily: 'var(--font-mono)' }}>
                      <span className={`stat-value ${Number(book.profitLoss) >= 0 ? 'positive' : 'negative'}`}
                            style={{ fontSize: '0.9rem' }}>
                        {Number(book.profitLoss) >= 0 ? '+' : ''}${Number(book.profitLoss)?.toFixed(2)}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
