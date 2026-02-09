import { Button, Text } from "@fluentui/react-components";

export default function ErrorState({
  title,
  detail,
  onRetry,
}: {
  title: string;
  detail?: string;
  onRetry?: () => void;
}) {
  return (
    <div style={{ padding: 24, border: "1px solid #E6E6E6", borderRadius: 12, background: "#FFF" }}>
      <Text weight="semibold">{title}</Text>
      {detail ? <div style={{ marginTop: 6, color: "#6B6B6B" }}>{detail}</div> : null}
      {onRetry ? (
        <div style={{ marginTop: 12 }}>
          <Button appearance="secondary" onClick={onRetry}>
            Reintentar
          </Button>
        </div>
      ) : null}
    </div>
  );
}