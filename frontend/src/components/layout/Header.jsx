import { useState } from 'react';
import { captureToday, resolveOutcomes } from '../../api/api';

/**
 * Top header bar with action buttons for manual capture and settlement.
 * Shows loading state while API calls are in progress.
 */
export default function Header() {
  const [loading, setLoading] = useState(null);
  const [message, setMessage] = useState(null);

  const handleCapture = async () => {
    setLoading('capture');
    setMessage(null);
    try {
      const { data } = await captureToday();
      setMessage({ type: 'success', text: `Captured ${data.totalPicks} predictions` });
    } catch (err) {
      setMessage({ type: 'error', text: 'Capture failed: ' + (err.response?.data?.error || err.message) });
    }
    setLoading(null);
  };

  const handleResolve = async () => {
    setLoading('resolve');
    setMessage(null);
    try {
      const { data } = await resolveOutcomes();
      setMessage({ type: 'success', text: `Settled ${data.settled} predictions` });
    } catch (err) {
      setMessage({ type: 'error', text: 'Resolution failed: ' + (err.response?.data?.error || err.message) });
    }
    setLoading(null);
  };

  return (
    <header style={styles.header}>
      <div style={styles.actions}>
        <button
          style={{ ...styles.btn, ...styles.btnCapture }}
          onClick={handleCapture}
          disabled={loading !== null}
        >
          {loading === 'capture' ? 'Capturing...' : '📥 Capture Today\'s Picks'}
        </button>
        <button
          style={{ ...styles.btn, ...styles.btnResolve }}
          onClick={handleResolve}
          disabled={loading !== null}
        >
          {loading === 'resolve' ? 'Resolving...' : '⚡ Resolve Outcomes'}
        </button>
      </div>

      {message && (
        <div style={{
          ...styles.message,
          background: message.type === 'success'
            ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
          color: message.type === 'success' ? '#10b981' : '#ef4444',
        }}>
          {message.text}
        </div>
      )}
    </header>
  );
}

const styles = {
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 24,
    flexWrap: 'wrap',
    gap: 12,
  },
  actions: {
    display: 'flex',
    gap: 10,
  },
  btn: {
    padding: '8px 16px',
    border: 'none',
    borderRadius: 8,
    fontWeight: 600,
    fontSize: '0.85rem',
    cursor: 'pointer',
    transition: 'all 0.2s',
    fontFamily: 'inherit',
  },
  btnCapture: {
    background: 'linear-gradient(135deg, #7c3aed, #6366f1)',
    color: 'white',
  },
  btnResolve: {
    background: 'rgba(16, 185, 129, 0.15)',
    color: '#10b981',
    border: '1px solid rgba(16, 185, 129, 0.3)',
  },
  message: {
    padding: '8px 16px',
    borderRadius: 8,
    fontSize: '0.85rem',
    fontWeight: 500,
  },
};
