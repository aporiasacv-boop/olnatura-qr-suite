import * as React from "react";
import {
  Button,
  makeStyles,
  shorthands,
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableHeaderCell,
  TableRow,
  Text,
} from "@fluentui/react-components";
import { api, ApiError, API_BASE } from "../api/client";
import { useToasts } from "../components/ui/toasts";
import AppCard from "../components/ui/AppCard";
import AuditDetailCell from "../components/ui/AuditDetailCell";
import { brand } from "../styles/brand";
import { LABELS, formatDateTime, actionTypeDisplay } from "../utils/displayLabels";

type DailyPoint = { date: string; labelsCreated: number; scans: number };

type RecentActivity = {
  id: string;
  createdAt: string;
  actionType: string;
  actorEmail?: string;
  actorRol?: string;
  lote?: string;
  metadata?: Record<string, unknown>;
  deviceId?: string;
};

type LastPowerBiExport = {
  exportedAt?: string | null;
  actorEmail?: string | null;
  labelsExported?: number | null;
  scansExported?: number | null;
  auditsExported?: number | null;
  usersExported?: number | null;
};

type OperationalMetrics = {
  generatedAt: string;
  rangeDays: number;
  summary: {
    labelsCreatedToday: number;
    scansToday: number;
    activeLots: number;
    auditEventsInRange: number;
  };
  dailySeries: DailyPoint[];
  recentActivity: RecentActivity[];
  lastPowerBiExport?: LastPowerBiExport | null;
};

const useStyles = makeStyles({
  wrap: { display: "grid", gap: "28px" },
  headerRow: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: "12px",
    flexWrap: "wrap",
  },
  title: { fontSize: "20px", fontWeight: 600, color: brand.text, margin: 0 },
  subtitle: { fontSize: "13px", color: brand.muted, marginTop: "4px" },
  sectionTitle: { fontSize: "16px", fontWeight: 600, color: brand.text, margin: "0 0 12px" },
  kpiGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
    ...shorthands.gap("14px"),
  },
  kpiValue: {
    fontSize: "32px",
    fontWeight: 700,
    color: brand.text,
    lineHeight: 1.1,
    marginTop: "8px",
  },
  kpiHint: { fontSize: "12px", color: brand.muted, marginTop: "8px" },
  chartsGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
    ...shorthands.gap("14px"),
  },
  chartHint: { fontSize: "12px", color: brand.muted, marginBottom: "12px" },
  muted: { color: brand.muted },
  powerBiCard: {
    display: "grid",
    gap: "10px",
  },
  powerBiWhen: {
    fontSize: "18px",
    fontWeight: 600,
    color: brand.text,
    marginTop: "4px",
  },
  powerBiStats: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(160px, 1fr))",
    gap: "10px",
    marginTop: "4px",
  },
  powerBiStatLabel: { fontSize: "12px", color: brand.muted },
  powerBiStatValue: { fontSize: "16px", fontWeight: 600, color: brand.text, marginTop: "2px" },
});

function formatCount(n?: number | null): string {
  if (n == null || Number.isNaN(Number(n))) return "—";
  return Number(n).toLocaleString("es-MX");
}

function formatDayLabel(isoDate: string): string {
  const parts = isoDate.split("-");
  if (parts.length !== 3) return isoDate;
  return `${parts[1]}-${parts[2]}`;
}

function SimpleBarChart({
  points,
  valueKey,
}: {
  points: DailyPoint[];
  valueKey: "labelsCreated" | "scans";
}) {
  const max = Math.max(1, ...points.map((p) => Number(p[valueKey]) || 0));
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: `repeat(${Math.max(points.length, 1)}, 1fr)`,
        alignItems: "end",
        gap: 8,
        height: 160,
        paddingTop: 8,
      }}
      role="img"
      aria-label="Gráfica de barras"
    >
      {points.map((p) => {
        const value = Number(p[valueKey]) || 0;
        const heightPct = Math.max(value > 0 ? 8 : 2, Math.round((value / max) * 100));
        return (
          <div key={p.date} style={{ display: "grid", gap: 6, alignContent: "end", minWidth: 0 }}>
            <div
              style={{
                height: 120,
                display: "flex",
                alignItems: "flex-end",
                justifyContent: "center",
              }}
            >
              <div
                title={`${formatDayLabel(p.date)}: ${value}`}
                style={{
                  width: "70%",
                  maxWidth: 36,
                  height: `${heightPct}%`,
                  backgroundColor: brand.primary,
                  borderRadius: "4px 4px 0 0",
                  position: "relative",
                  minHeight: value > 0 ? 4 : 2,
                  opacity: value > 0 ? 1 : 0.25,
                }}
              >
                {value > 0 ? (
                  <span
                    style={{
                      position: "absolute",
                      top: -18,
                      left: "50%",
                      transform: "translateX(-50%)",
                      fontSize: 11,
                      fontWeight: 600,
                      color: brand.text2,
                    }}
                  >
                    {value}
                  </span>
                ) : null}
              </div>
            </div>
            <Text
              style={{
                fontSize: 11,
                color: brand.muted,
                textAlign: "center",
                whiteSpace: "nowrap",
              }}
            >
              {formatDayLabel(p.date)}
            </Text>
          </div>
        );
      })}
    </div>
  );
}

export default function AdminMetricsPage() {
  const s = useStyles();
  const toasts = useToasts();
  const [data, setData] = React.useState<OperationalMetrics | null>(null);
  const [busy, setBusy] = React.useState(false);
  const [exporting, setExporting] = React.useState(false);
  const [lastUpdatedAt, setLastUpdatedAt] = React.useState<Date | null>(null);

  const load = React.useCallback(async () => {
    setBusy(true);
    try {
      const res = await api<OperationalMetrics>("/admin/metrics?days=7", { toast: false });
      setData(res);
      setLastUpdatedAt(new Date());
    } catch (err) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "Error al cargar métricas",
        message: ae?.message ?? "Revisa permisos o conexión.",
        error: ae,
      });
    } finally {
      setBusy(false);
    }
  }, [toasts]);

  React.useEffect(() => {
    load();
  }, [load]);

  const exportPowerBi = async () => {
    setExporting(true);
    try {
      const base = API_BASE.replace(/\/+$/, "");
      const url = `${base}/api/v1/admin/metrics/export/powerbi`;
      const res = await fetch(url, { method: "GET", credentials: "include" });
      if (!res.ok) {
        throw new Error(res.status === 403 ? "Sin permiso (solo ADMIN)" : `HTTP ${res.status}`);
      }
      const blob = await res.blob();
      const href = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = href;
      const cd = res.headers.get("Content-Disposition") || "";
      const match = /filename="?([^"]+)"?/i.exec(cd);
      a.download = match?.[1] || "Executive_Dashboard_Export.xlsx";
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(href);
      toasts.push({
        intent: "success",
        title: "Excel descargado",
        message: "Executive_Dashboard_Export.xlsx listo para Power BI.",
      });
      await load();
    } catch (err) {
      toasts.push({
        intent: "error",
        title: "No se pudo exportar",
        message: err instanceof Error ? err.message : "Intenta de nuevo.",
      });
    } finally {
      setExporting(false);
    }
  };

  const summary = data?.summary;
  const series = data?.dailySeries ?? [];
  const recent = data?.recentActivity ?? [];
  const lastExport = data?.lastPowerBiExport ?? null;
  const updateLabel = lastUpdatedAt
    ? `Actualizar · ${lastUpdatedAt.toLocaleTimeString("es-MX", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      })}`
    : "Actualizar";

  return (
    <div className={s.wrap}>
      <div className={s.headerRow}>
        <div>
          <h1 className={s.title}>{LABELS.metrics}</h1>
          <div className={s.subtitle}>Últimos {data?.rangeDays ?? 7} días</div>
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <Button
            appearance="secondary"
            onClick={exportPowerBi}
            disabled={exporting || busy}
          >
            {exporting ? "Exportando…" : "Exportar datos para Power BI"}
          </Button>
          <Button
            appearance="primary"
            onClick={load}
            disabled={busy}
            title={
              lastUpdatedAt
                ? `Última actualización: ${lastUpdatedAt.toLocaleString("es-MX")}`
                : undefined
            }
          >
            {busy ? "Cargando…" : updateLabel}
          </Button>
        </div>
      </div>

      <section>
        <AppCard>
          <div className={s.powerBiCard}>
            <Text weight="semibold">Última exportación Power BI</Text>
            {lastExport?.exportedAt ? (
              <>
                <div className={s.powerBiWhen}>
                  {(() => {
                    const dt = formatDateTime(lastExport.exportedAt);
                    return `${dt.date} ${dt.time}`;
                  })()}
                </div>
                {lastExport.actorEmail ? (
                  <Text className={s.muted} size={200}>
                    Por {lastExport.actorEmail}
                  </Text>
                ) : null}
                <div className={s.powerBiStats}>
                  <div>
                    <div className={s.powerBiStatLabel}>Etiquetas exportadas</div>
                    <div className={s.powerBiStatValue}>{formatCount(lastExport.labelsExported)}</div>
                  </div>
                  <div>
                    <div className={s.powerBiStatLabel}>Escaneos exportados</div>
                    <div className={s.powerBiStatValue}>{formatCount(lastExport.scansExported)}</div>
                  </div>
                  <div>
                    <div className={s.powerBiStatLabel}>Auditorías exportadas</div>
                    <div className={s.powerBiStatValue}>{formatCount(lastExport.auditsExported)}</div>
                  </div>
                  <div>
                    <div className={s.powerBiStatLabel}>Usuarios</div>
                    <div className={s.powerBiStatValue}>{formatCount(lastExport.usersExported)}</div>
                  </div>
                </div>
              </>
            ) : (
              <Text className={s.muted}>
                Aún no hay exportaciones. Usa “Exportar datos para Power BI” para generar el
                Excel ejecutivo.
              </Text>
            )}
          </div>
        </AppCard>
      </section>

      <section>
        <h2 className={s.sectionTitle}>Resumen hoy y estado</h2>
        <div className={s.kpiGrid}>
          <AppCard>
            <Text weight="semibold">Etiquetas registradas hoy</Text>
            <div className={s.kpiValue}>{summary?.labelsCreatedToday ?? "—"}</div>
          </AppCard>
          <AppCard>
            <Text weight="semibold">Escaneos hoy</Text>
            <div className={s.kpiValue}>{summary?.scansToday ?? "—"}</div>
          </AppCard>
          <AppCard>
            <Text weight="semibold">Lotes activos</Text>
            <div className={s.kpiValue}>{summary?.activeLots ?? "—"}</div>
          </AppCard>
          <AppCard>
            <Text weight="semibold">Auditoría (7 días)</Text>
            <div className={s.kpiValue}>{summary?.auditEventsInRange ?? "—"}</div>
          </AppCard>
        </div>
      </section>

      <section>
        <h2 className={s.sectionTitle}>Tendencia últimos 7 días</h2>
        <div className={s.chartsGrid}>
          <AppCard>
            <Text weight="semibold">Etiquetas por día</Text>
            {series.length === 0 ? (
              <Text className={s.muted}>Sin datos</Text>
            ) : (
              <SimpleBarChart points={series} valueKey="labelsCreated" />
            )}
          </AppCard>
          <AppCard>
            <Text weight="semibold">Escaneos por día</Text>
            {series.length === 0 ? (
              <Text className={s.muted}>Sin datos</Text>
            ) : (
              <SimpleBarChart points={series} valueKey="scans" />
            )}
          </AppCard>
        </div>
      </section>

      <section>
        <h2 className={s.sectionTitle}>Actividad reciente</h2>
        <AppCard>
          <div style={{ overflowX: "auto" }}>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHeaderCell>{LABELS.fecha}</TableHeaderCell>
                  <TableHeaderCell>{LABELS.accion}</TableHeaderCell>
                  <TableHeaderCell>{LABELS.usuario}</TableHeaderCell>
                  <TableHeaderCell>Lote</TableHeaderCell>
                  <TableHeaderCell>{LABELS.detalle}</TableHeaderCell>
                </TableRow>
              </TableHeader>
              <TableBody>
                {recent.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5}>
                      <Text className={s.muted}>{LABELS.noEvents}</Text>
                    </TableCell>
                  </TableRow>
                ) : (
                  recent.map((ev) => {
                    const { date, time } = formatDateTime(ev.createdAt);
                    return (
                      <TableRow key={ev.id} className="table-hover-row">
                        <TableCell>
                          <div style={{ whiteSpace: "nowrap" }}>
                            <div>{date}</div>
                            <div style={{ fontSize: 12, color: brand.muted }}>{time}</div>
                          </div>
                        </TableCell>
                        <TableCell>{actionTypeDisplay(ev.actionType)}</TableCell>
                        <TableCell>
                          <div>{ev.actorEmail || "—"}</div>
                          {ev.actorRol ? (
                            <div style={{ fontSize: 12, color: brand.muted }}>{ev.actorRol}</div>
                          ) : null}
                        </TableCell>
                        <TableCell>{ev.lote || "—"}</TableCell>
                        <TableCell>
                          <AuditDetailCell
                            metadata={ev.metadata}
                            deviceId={ev.deviceId}
                            actionType={ev.actionType}
                          />
                        </TableCell>
                      </TableRow>
                    );
                  })
                )}
              </TableBody>
            </Table>
          </div>
        </AppCard>
      </section>
    </div>
  );
}
