import { useDebugMode } from "main/utils/useDebugMode";

export default function Footer() {
  const { debugMode, setDebugMode, canUseDebugMode } = useDebugMode();

  return (
    <footer
      style={{
        background: "#ffffff",
        borderTop: "1px solid var(--border)",
      }}
    >
      <div
        style={{
          width: "1126px",
          maxWidth: "100%",
          margin: "0 auto",
          padding: "16px 20px",
          color: "#475569",
          fontSize: "0.95rem",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          flexWrap: "wrap",
          gap: "8px",
        }}
      >
        <span>Scaffold is a UCSB Computer Science project.</span>
        {canUseDebugMode && (
          <label
            data-testid="debug-mode-toggle-label"
            style={{
              display: "flex",
              alignItems: "center",
              gap: "6px",
              cursor: "pointer",
              userSelect: "none",
            }}
          >
            <input
              type="checkbox"
              data-testid="debug-mode-toggle"
              checked={debugMode}
              onChange={(e) => setDebugMode(e.target.checked)}
            />
            Debug Mode
          </label>
        )}
      </div>
    </footer>
  );
}
