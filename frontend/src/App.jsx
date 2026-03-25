import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import ProtectedRoute from './auth/ProtectedRoute';
import Sidebar from './components/layout/Sidebar';
import LoginPage from './pages/LoginPage';
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
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/*" element={
            <ProtectedRoute>
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
            </ProtectedRoute>
          } />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
