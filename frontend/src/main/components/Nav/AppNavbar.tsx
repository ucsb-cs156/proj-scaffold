import { Link } from 'react-router-dom';

export default function AppNavbar() {
  return (
    <header
      style={{
        background: '#ffffff',
        borderBottom: '1px solid var(--border)',
      }}
    >
      <div
        style={{
          width: '1126px',
          maxWidth: '100%',
          margin: '0 auto',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
          padding: '16px 20px',
        }}
      >
        <Link
          to="/"
          style={{
            color: '#1E293B',
            fontSize: '1.5rem',
            fontWeight: 700,
            textDecoration: 'none',
          }}
        >
          Scaffold
        </Link>
        <span
          style={{
            color: '#475569',
            fontSize: '0.95rem',
          }}
        >
          UCSB CS concept graph
        </span>
      </div>
    </header>
  );
}
