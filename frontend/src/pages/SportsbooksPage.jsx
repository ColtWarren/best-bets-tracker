import { useState, useEffect } from 'react';
import { getActiveSportsbooks } from '../api/api';

/**
 * Missouri sportsbooks page — shows all licensed sportsbooks
 * with their current signup bonuses.
 */
export default function SportsbooksPage() {
  const [sportsbooks, setSportsbooks] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadSportsbooks();
  }, []);

  const loadSportsbooks = async () => {
    try {
      const { data } = await getActiveSportsbooks();
      setSportsbooks(data);
    } catch (err) {
      console.error('Failed to load sportsbooks:', err);
    }
    setLoading(false);
  };

  if (loading) return <div className="loading">Loading sportsbooks</div>;

  return (
    <div>
      <div className="page-header">
        <h1>Missouri Sportsbooks</h1>
        <p>Licensed sportsbooks available for legal betting in Missouri</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 16 }}>
        {sportsbooks.map((book) => (
          <div className="card" key={book.id} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ fontSize: '1.1rem' }}>{book.name}</h3>
              <span className={`badge ${book.active ? 'badge-won' : 'badge-lost'}`}>
                {book.active ? 'Active' : 'Inactive'}
              </span>
            </div>
            {book.signupBonus && (
              <div style={{
                padding: '10px 14px',
                background: 'rgba(124, 58, 237, 0.1)',
                borderRadius: 8,
                fontSize: '0.85rem',
                color: 'var(--accent-purple-light)',
              }}>
                {book.signupBonus}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
