import { NavLink } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';

/**
 * Fixed left sidebar with navigation links.
 * Active link gets a purple highlight matching the cyberpunk theme.
 */
export default function Sidebar() {
  const { user, logout } = useAuth();

  const navItems = [
    { path: '/',              icon: '\u{1F4CA}', label: 'Dashboard' },
    { path: '/predictions',   icon: '\u{1F3AF}', label: 'Predictions' },
    { path: '/accuracy',      icon: '\u{1F4C8}', label: 'Accuracy' },
    { path: '/profitability', icon: '\u{1F4B0}', label: 'Profitability' },
    { path: '/snapshots',     icon: '\u{1F4C5}', label: 'Daily History' },
    { path: '/sportsbooks',   icon: '\u{1F3DB}\uFE0F', label: 'Sportsbooks' },
  ];

  return (
    <aside style={styles.sidebar}>
      <div style={styles.logo}>
        <span style={styles.logoIcon}>{'\u{1F3B2}'}</span>
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
        {user && (
          <div style={styles.userInfo}>
            {user.picture && (
              <img src={user.picture} alt="" style={styles.avatar} referrerPolicy="no-referrer" />
            )}
            <div style={styles.userName}>{user.name}</div>
            <button onClick={logout} style={styles.logoutBtn}>Sign out</button>
          </div>
        )}
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
  userInfo: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 6,
    marginBottom: 12,
    paddingBottom: 12,
    borderBottom: '1px solid rgba(124, 58, 237, 0.1)',
  },
  avatar: {
    width: 32,
    height: 32,
    borderRadius: '50%',
    border: '2px solid rgba(124, 58, 237, 0.3)',
  },
  userName: {
    fontSize: '0.8rem',
    color: '#94a3b8',
  },
  logoutBtn: {
    background: 'none',
    border: '1px solid rgba(124, 58, 237, 0.2)',
    borderRadius: 6,
    color: '#64748b',
    fontSize: '0.7rem',
    padding: '4px 12px',
    cursor: 'pointer',
    transition: 'all 0.2s',
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
