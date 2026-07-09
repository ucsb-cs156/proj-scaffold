import { useStaffTools } from "main/utils/useStaffTools";

const toggleLabelStyle = {
  display: "flex",
  alignItems: "center",
  gap: "6px",
  cursor: "pointer",
  userSelect: "none",
} as const;

export default function Footer() {
  const { debugMode, unlockSubconcepts, canUseStaffTools, setStaffTool } =
    useStaffTools();

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
        {canUseStaffTools && (
          <span style={{ display: "flex", gap: "16px" }}>
            <label
              data-testid="unlock-subconcepts-toggle-label"
              style={toggleLabelStyle}
            >
              <input
                type="checkbox"
                data-testid="unlock-subconcepts-toggle"
                checked={unlockSubconcepts}
                onChange={(e) =>
                  setStaffTool("unlockSubconcepts", e.target.checked)
                }
              />
              Unlock Subconcepts
            </label>
            <label
              data-testid="debug-mode-toggle-label"
              style={toggleLabelStyle}
            >
              <input
                type="checkbox"
                data-testid="debug-mode-toggle"
                checked={debugMode}
                onChange={(e) => setStaffTool("debugMode", e.target.checked)}
              />
              Debug Mode
            </label>
          </span>
        )}
      </div>
    </footer>
  );
}
