import { NavLink } from 'react-router-dom';

/**
 * Fixed left sidebar with navigation links.
 * Active link gets a purple highlight matching the cyberpunk theme.
 */
export default function Sidebar() {
  const navItems = [
    { path: '/',              icon: '📊', label: 'Dashboard' },
    { path: '/predictions',   icon: '🎯', label: 'Predictions' },
    { path: '/accuracy',      icon: '📈', label: 'Accuracy' },
    { path: '/profitability', icon: '💰', label: 'Profitability' },
    { path: '/snapshots',     icon: '📅', label: 'Daily History' },
    { path: '/sportsbooks',   icon: '🏛️', label: 'Sportsbooks' },
  ];

  return (
    <aside style={styles.sidebar}>
      <div style={styles.logo}>
        <span style={styles.logoIcon}>🎲</span>
        <div>
          <div style={styles.logoTitle}>Best Bets</div>
          <div style={styles.logoSubtitle}>Tracker</div>
        </div>
      </div>

      <nav style={styles.nav}>
        {navItems.map(({ path, icon, label }) => (
          <NavLink
            key={path}
            to={path}
            end={path === '/'}
            style={({ isActive }) => ({
              ...styles.navLink,
              ...(isActive ? styles.navLinkActive : {}),
            })}
          >
            <span style={styles.navIcon}>{icon}</span>
            {label}
          </NavLink>
        ))}
      </nav>

      <div style={styles.footer}>
        <div style={styles.footerText}>Missouri Legal</div>
        <div style={styles.footerSub}>Sportsbook Tracker</div>
      </div>
    </aside>
  );
}

const styles = {
  sidebar: {
    position: 'fixed',
    left: 0,
    top: 0,
    width: 260,
    height: '100vh',
    background: 'linear-gradient(180deg, #111638 0%, #0a0e27 100%)',
    borderRight: '1px solid rgba(124, 58, 237, 0.2)',
    display: 'flex',
    flexDirection: 'column',
    zIndex: 100,
  },
  logo: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    padding: '24px 20px',
    borderBottom: '1px solid rgba(124, 58, 237, 0.15)',
  },
  logoIcon: {
    fontSize: '2rem',
  },
  logoTitle: {
    fontSize: '1.1rem',
    fontWeight: 700,
    background: 'linear-gradient(135deg, #7c3aed, #6366f1)',
    WebkitBackgroundClip: 'text',
    WebkitTextFillColor: 'transparent',
  },
  logoSubtitle: {
    fontSize: '0.7rem',
    color: '#64748b',
    textTransform: 'uppercase',
    letterSpacing: 2,
  },
  nav: {
    flex: 1,
    padding: '16px 12px',
    display: 'flex',
    flexDirection: 'column',
    gap: 4,
  },
  navLink: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    padding: '10px 14px',
    borderRadius: 8,
    color: '#94a3b8',
    textDecoration: 'none',
    fontSize: '0.9rem',
    transition: 'all 0.2s',
  },
  navLinkActive: {
    background: 'rgba(124, 58, 237, 0.15)',
    color: '#a78bfa',
    borderLeft: '3px solid #7c3aed',
  },
  navIcon: {
    fontSize: '1.1rem',
    width: 24,
    textAlign: 'center',
  },
  footer: {
    padding: '16px 20px',
    borderTop: '1px solid rgba(124, 58, 237, 0.15)',
    textAlign: 'center',
  },
  footerText: {
    fontSize: '0.75rem',
    color: '#64748b',
  },
  footerSub: {
    fontSize: '0.65rem',
    color: '#475569',
    marginTop: 2,
  },
};
