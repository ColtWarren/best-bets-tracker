import { useState, useEffect } from 'react';
import { getDashboardData, getRecentOutcomes, getStreak } from '../api/api';
import Header from '../components/layout/Header';
import OverviewCards from '../components/dashboard/OverviewCards';
import RecentResults from '../components/dashboard/RecentResults';
import SportBreakdown from '../components/accuracy/SportBreakdown';
import TrendComparison from '../components/profitability/TrendComparison';

/**
 * Main dashboard page — the first thing users see.
 * Combines all key metrics in a single view.
 */
export default function DashboardPage() {
  const [dashboard, setDashboard] = useState(null);
  const [outcomes, setOutcomes] = useState([]);
  const [streak, setStreak] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadDashboard();
  }, []);

  const loadDashboard = async () => {
    setLoading(true);
    try {
      const [dashRes, outcomesRes, streakRes] = await Promise.all([
        getDashboardData(),
        getRecentOutcomes(10),
        getStreak(),
      ]);
      setDashboard(dashRes.data);
      setOutcomes(outcomesRes.data);
      setStreak(streakRes.data);
    } catch (err) {
      setError(err.message);
    }
    setLoading(false);
  };

  if (loading) return <div className="loading">Loading dashboard</div>;
  if (error) return <div className="empty-state"><h3>Error: {error}</h3></div>;

  return (
    <div>
      <div className="page-header">
        <h1>Command Center</h1>
        <p>AI Best Bets Accuracy Tracker</p>
      </div>

      <Header />

      <OverviewCards stats={dashboard?.overall} streak={streak} />

      <div className="grid-2">
        <SportBreakdown data={dashboard?.bySport} />
        <TrendComparison trends={dashboard?.trends} />
      </div>

      <RecentResults outcomes={outcomes} />
    </div>
  );
}
