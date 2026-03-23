import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Sidebar from './components/layout/Sidebar';
import DashboardPage from './pages/DashboardPage';
import PredictionsPage from './pages/PredictionsPage';
import AccuracyPage from './pages/AccuracyPage';
import ProfitabilityPage from './pages/ProfitabilityPage';
import SnapshotsPage from './pages/SnapshotsPage';
import SportsbooksPage from './pages/SportsbooksPage';
import './styles/theme.css';

export default function App() {
  return (
    <BrowserRouter>
      <div className="app-container">
        <Sidebar />
        <main className="main-content">
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/predictions" element={<PredictionsPage />} />
            <Route path="/accuracy" element={<AccuracyPage />} />
            <Route path="/profitability" element={<ProfitabilityPage />} />
            <Route path="/snapshots" element={<SnapshotsPage />} />
            <Route path="/sportsbooks" element={<SportsbooksPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
