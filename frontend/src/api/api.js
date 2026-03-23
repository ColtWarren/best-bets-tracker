import axios from 'axios';

/**
 * Axios instance pre-configured to hit the Spring Boot backend on port 8081.
 * All API calls go through this instance for consistent base URL and error handling.
 */
const api = axios.create({
  baseURL: 'http://localhost:8081/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// === Predictions ===

export const captureToday = () => api.post('/predictions/capture');

export const getPredictions = (params = {}) => api.get('/predictions', { params });

export const getTodaysPredictions = () => api.get('/predictions/today');

export const getPendingPredictions = () => api.get('/predictions/pending');

export const getRecentPredictions = (limit = 20) =>
  api.get('/predictions/recent', { params: { limit } });

export const getPredictionById = (id) => api.get(`/predictions/${id}`);

// === Outcomes ===

export const resolveOutcomes = () => api.post('/outcomes/resolve');

export const getRecentOutcomes = (limit = 20) =>
  api.get('/outcomes/recent', { params: { limit } });

export const getOutcomeByPrediction = (predictionId) =>
  api.get(`/outcomes/prediction/${predictionId}`);

// === Analytics ===

export const getOverallAccuracy = () => api.get('/analytics/accuracy');

export const getAccuracyBySport = () => api.get('/analytics/accuracy/by-sport');

export const getAccuracyByBetType = () => api.get('/analytics/accuracy/by-type');

export const getAccuracyByConfidence = () => api.get('/analytics/accuracy/by-confidence');

export const getTrend = (days = 30) => api.get('/analytics/trend', { params: { days } });

export const getTrendComparison = () => api.get('/analytics/trend/comparison');

export const getDashboardData = () => api.get('/analytics/dashboard');

export const generateReports = () => api.post('/analytics/reports/generate');

// === Snapshots ===

export const getSnapshots = () => api.get('/snapshots');

export const getSnapshotByDate = (date) => api.get(`/snapshots/${date}`);

export const getSnapshotAggregate = () => api.get('/snapshots/aggregate');

export const getStreak = () => api.get('/snapshots/streak');

export const getUnresolvedSnapshots = () => api.get('/snapshots/unresolved');

// === Simulation ===

export const createSimulatedBets = (strategy = 'flat', stake = 50, bankroll = 1000) =>
  api.post('/simulation/create-bets', null, { params: { strategy, stake, bankroll } });

export const settleSimulatedBets = () => api.post('/simulation/settle');

export const getBankrollSummary = (startingBankroll = 1000) =>
  api.get('/simulation/bankroll', { params: { startingBankroll } });

export const getProfitBySportsbook = () => api.get('/simulation/by-sportsbook');

// === Sportsbooks ===

export const getActiveSportsbooks = () => api.get('/sportsbooks/active');

export default api;
