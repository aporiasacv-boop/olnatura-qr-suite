import { Spinner } from "@fluentui/react-components";

export default function LoadingState({ label = "Cargando…" }: { label?: string }) {
  return (
    <div style={{ padding: 24, display: "flex", alignItems: "center", gap: 12 }}>
      <Spinner size="medium" />
      <div>{label}</div>
    </div>
  );
}