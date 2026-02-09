import React from "react";
import { Tag } from "@fluentui/react-components";

export default function StatusTag({ status }: { status: string }) {
  const s = status?.toUpperCase?.() ?? status;


  const appearance =
    s === "APROBADO" ? "filled" :
    s === "CUARENTENA" ? "outline" :
    s === "RECHAZADO" ? "filled" :
    "outline";

  return <Tag appearance={appearance}>{s}</Tag>;
}