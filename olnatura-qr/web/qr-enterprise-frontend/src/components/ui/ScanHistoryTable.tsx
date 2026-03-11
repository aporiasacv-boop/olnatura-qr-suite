import React from "react";
import { LABELS, formatDateTime } from "../../utils/displayLabels";

function pick(ev: any, keys: string[], fallback = "—") {
  for (const k of keys) {
    const v = ev?.[k];
    if (typeof v === "string" && v.trim()) return v;
    if (typeof v === "number") return String(v);
    if (v != null && typeof v === "object" && !Array.isArray(v)) return String(v);
  }
  return fallback;
}

export default function ScanHistoryTable({ events }: { events: Record<string, any>[] }) {
  return (
    <div style={{ border: "1px solid #E6E6E6", borderRadius: 12, overflow: "hidden" }}>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 0.8fr 1fr 1fr 0.8fr 1fr",
          padding: "10px 12px",
          background: "#F6F7F8",
          fontWeight: 600,
        }}
      >
        <div>{LABELS.fecha}</div>
        <div>{LABELS.hora}</div>
        <div>{LABELS.usuario}</div>
        <div>{LABELS.dispositivo}</div>
        <div>{LABELS.accion}</div>
        <div>{LABELS.detalle}</div>
      </div>

      {events.map((ev, idx) => {
        const iso = pick(ev, ["createdAt", "fecha", "timestamp"]);
        const { date, time } = formatDateTime(iso !== "—" ? iso : undefined);
        const usuario = pick(ev, ["usuario", "user", "username", "scannedBy"]);
        const dispositivo = pick(ev, ["deviceId", "dispositivo", "device"]);
        const detalle = pick(ev, ["lote", "ubicacion", "location"]);
        return (
          <div
            key={ev?.id ?? idx}
            style={{
              display: "grid",
              gridTemplateColumns: "1fr 0.8fr 1fr 1fr 0.8fr 1fr",
              padding: "10px 12px",
              borderTop: "1px solid #EFEFEF",
            }}
          >
            <div>{date}</div>
            <div>{time}</div>
            <div>{usuario !== "—" ? usuario : LABELS.noData}</div>
            <div>{dispositivo !== "—" ? dispositivo : LABELS.noData}</div>
            <div>Escaneo</div>
            <div>{detalle !== "—" ? detalle : ""}</div>
          </div>
        );
      })}
    </div>
  );
}
