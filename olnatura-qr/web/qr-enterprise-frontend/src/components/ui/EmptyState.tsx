import { Text } from "@fluentui/react-components";

export default function EmptyState({ title, hint }: { title: string; hint?: string }) {
  return (
    <div style={{ padding: 24, border: "1px solid #E6E6E6", borderRadius: 12, background: "#FFF" }}>
      <Text weight="semibold">{title}</Text>
      {hint ? <div style={{ marginTop: 6, color: "#6B6B6B" }}>{hint}</div> : null}
    </div>
  );
}