import { Link } from "react-router-dom";
import { hasRole, useCurrentUser, useLogout } from "../../utils/currentUser";
import { useSystemInfo } from "../../utils/systemInfo";

export default function AppNavbar() {
  const currentUser = useCurrentUser();
  const logout = useLogout();
  const { data: systemInfo } = useSystemInfo();

  const handleLogin = () => {
    window.location.href =
      systemInfo?.oauthLogin ?? "/oauth2/authorization/google";
  };

  const handleLogout = () => {
    logout.mutate();
  };

  return (
    <header
      style={{
        background: "#ffffff",
        borderBottom: "1px solid var(--border)",
      }}
    >
      <div
        style={{
          width: "1126px",
          maxWidth: "100%",
          margin: "0 auto",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          gap: "16px",
          padding: "16px 20px",
        }}
      >
        <Link
          to="/"
          style={{
            color: "#1E293B",
            fontSize: "1.5rem",
            fontWeight: 700,
            textDecoration: "none",
          }}
        >
          Scaffold
        </Link>
        <span
          style={{
            color: "#475569",
            fontSize: "0.95rem",
          }}
        >
          UCSB CS concept graph
        </span>
        <div
          style={{
            marginLeft: "auto",
            display: "flex",
            alignItems: "center",
            gap: "12px",
          }}
        >
          {hasRole(currentUser, "ROLE_ADMIN") && (
            <details>
              <summary
                style={{
                  cursor: "pointer",
                  color: "#1E293B",
                  fontSize: "0.9rem",
                  fontWeight: 600,
                }}
              >
                Admin
              </summary>
              <div
                style={{
                  position: "absolute",
                  marginTop: "6px",
                  background: "#ffffff",
                  border: "1px solid var(--border)",
                  borderRadius: "6px",
                  padding: "8px 10px",
                }}
              >
                <Link to="/admin/developer">Developer Info</Link>
              </div>
            </details>
          )}
          {currentUser?.loggedIn ? (
            <>
              <span style={{ color: "#475569", fontSize: "0.9rem" }}>
                {
                  (currentUser.root as { user?: { email?: string } })?.user
                    ?.email
                }
              </span>
              <button
                onClick={handleLogout}
                style={{
                  padding: "6px 14px",
                  background: "#1E293B",
                  color: "#ffffff",
                  border: "none",
                  borderRadius: "6px",
                  cursor: "pointer",
                  fontSize: "0.875rem",
                  fontWeight: 600,
                }}
              >
                Log Out
              </button>
            </>
          ) : (
            <button
              onClick={handleLogin}
              style={{
                padding: "6px 14px",
                background: "#1E293B",
                color: "#ffffff",
                border: "none",
                borderRadius: "6px",
                cursor: "pointer",
                fontSize: "0.875rem",
                fontWeight: 600,
              }}
            >
              Log In
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
