export default function AssignConceptsToggle({
  active,
  disabled,
  onClick,
}: {
  active: boolean;
  disabled: boolean;
  onClick: () => void;
}) {
  return (
    <button
      data-testid="AssignConceptsToggle"
      onClick={onClick}
      disabled={disabled}
      style={{
        height: 28,
        padding: "0px 10px",
        fontFamily: "Helvetica, Arial, sans-serif",
        fontSize: 13,
        background: active ? "#1E293B" : "#ffffff",
        color: active ? "#ffffff" : "#1E293B",
        border: "1px solid #000000",
        borderRadius: 6,
        cursor: disabled ? "not-allowed" : "pointer",
        opacity: disabled ? 0.5 : 1,
        whiteSpace: "nowrap",
      }}
    >
      Assign Concepts
    </button>
  );
}
