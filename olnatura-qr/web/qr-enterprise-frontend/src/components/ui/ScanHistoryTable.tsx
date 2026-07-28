import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableHeaderCell,
  TableRow,
} from "@fluentui/react-components";
import { LABELS, formatDateTime } from "../../utils/displayLabels";
import { displayUserIdentity, translateAuditAction } from "../../utils/auditActionTranslator";
import {
  TABLE_FIXED_STYLE,
  TABLE_SCROLL_WRAP,
  TRUNCATE_CELL,
  cellTitle,
} from "../../utils/tablePresentation";

function pick(ev: Record<string, unknown>, keys: string[], fallback = "—") {
  for (const k of keys) {
    const v = ev?.[k];
    if (typeof v === "string" && v.trim()) return v;
    if (typeof v === "number") return String(v);
  }
  return fallback;
}

export default function ScanHistoryTable({ events }: { events: Record<string, unknown>[] }) {
  return (
    <div style={TABLE_SCROLL_WRAP}>
      <Table aria-label={LABELS.scanHistory} style={{ ...TABLE_FIXED_STYLE, minWidth: 680 }}>
        <TableHeader>
          <TableRow>
            <TableHeaderCell style={{ width: "11%" }}>{LABELS.fecha}</TableHeaderCell>
            <TableHeaderCell style={{ width: "9%" }}>{LABELS.hora}</TableHeaderCell>
            <TableHeaderCell style={{ width: "24%" }}>{LABELS.usuario}</TableHeaderCell>
            <TableHeaderCell style={{ width: "14%" }}>{LABELS.rol}</TableHeaderCell>
            <TableHeaderCell style={{ width: "22%" }}>{LABELS.accion}</TableHeaderCell>
            <TableHeaderCell style={{ width: "20%" }}>Lote</TableHeaderCell>
          </TableRow>
        </TableHeader>
        <TableBody>
          {events.map((ev, idx) => {
            const iso = pick(ev, ["createdAt", "fecha", "timestamp"]);
            const { date, time } = formatDateTime(iso !== "—" ? iso : undefined);
            const userDisplay = pick(ev, ["userDisplay"], "");
            const username = pick(ev, ["username"], "");
            const usuario = displayUserIdentity(
              userDisplay !== "—" ? userDisplay : undefined,
              username !== "—" ? username : undefined
            );
            const rol = pick(ev, ["roleDisplay"], "—");
            const lote = pick(ev, ["lote"], "—");
            const accion = translateAuditAction("SCAN_QR");
            return (
              <TableRow key={String(ev?.id ?? idx)} className="table-hover-row">
                <TableCell style={{ whiteSpace: "nowrap" }}>{date}</TableCell>
                <TableCell style={{ whiteSpace: "nowrap" }}>{time}</TableCell>
                <TableCell style={TRUNCATE_CELL} title={cellTitle(usuario)}>
                  {usuario !== "—" ? usuario : LABELS.noData}
                </TableCell>
                <TableCell style={TRUNCATE_CELL} title={cellTitle(rol !== "—" ? rol : undefined)}>
                  {rol !== "—" ? rol : LABELS.noData}
                </TableCell>
                <TableCell style={TRUNCATE_CELL} title={cellTitle(accion)}>
                  {accion}
                </TableCell>
                <TableCell style={TRUNCATE_CELL} title={cellTitle(lote !== "—" ? lote : undefined)}>
                  {lote !== "—" ? lote : ""}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
