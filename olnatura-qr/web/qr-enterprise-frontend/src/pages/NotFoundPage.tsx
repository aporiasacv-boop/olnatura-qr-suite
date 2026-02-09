import React from "react";
import { Text } from "@fluentui/react-components";

export default function NotFoundPage() {
  return (
    <div style={{ padding: 24 }}>
      <Text weight="semibold" size={700}>404</Text>
      <div style={{ color: "#6B6B6B", marginTop: 6 }}>La ruta no existe.</div>
    </div>
  );
}