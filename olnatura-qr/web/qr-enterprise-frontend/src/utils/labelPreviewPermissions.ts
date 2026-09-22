import type { Role } from "../api/types";

export function canDownloadLabelPdf(hasRole: (r: Role) => boolean): boolean {
  return hasRole("ADMIN") || hasRole("ALMACEN");
}

export function canDownloadAuditPdf(hasRole: (r: Role) => boolean): boolean {
  return (
    hasRole("ADMIN") ||
    hasRole("ALMACEN") ||
    hasRole("PRODUCCION") ||
    hasRole("CALIDAD") ||
    hasRole("INSPECCION") ||
    hasRole("VALIDACION")
  );
}

export function canPrintLabel(hasRole: (r: Role) => boolean): boolean {
  return hasRole("ADMIN") || hasRole("ALMACEN");
}

export function parseEnvaseTotal(label: Record<string, unknown> | null | undefined): number {
  if (!label) return 1;
  const raw = label.envaseTotal ?? label.totalEnvases ?? 1;
  const n = typeof raw === "number" ? raw : parseInt(String(raw), 10);
  return Number.isFinite(n) && n >= 1 ? n : 1;
}

export function validateReprintRange(
  fromRaw: string,
  toRaw: string,
  envaseTotal: number,
  formatNumber: (n: number) => string
): { ok: true; from: number; to: number } | { ok: false; message: string } {
  const from = Number.parseInt(String(fromRaw).trim(), 10);
  const to = Number.parseInt(String(toRaw).trim(), 10);

  if (
    !Number.isFinite(from) ||
    !Number.isFinite(to) ||
    String(fromRaw).trim() === "" ||
    String(toRaw).trim() === ""
  ) {
    return {
      ok: false,
      message: `Indica un rango válido. Este lote tiene ${formatNumber(envaseTotal)} envase(s) registrado(s) (permitido: 1 a ${formatNumber(envaseTotal)}).`,
    };
  }
  if (!Number.isInteger(from) || !Number.isInteger(to)) {
    return { ok: false, message: "Desde y Hasta deben ser números enteros." };
  }
  if (from < 1 || to < 1) {
    return {
      ok: false,
      message: "El rango debe comenzar en 1. No se permiten números menores a 1.",
    };
  }
  if (from > envaseTotal || to > envaseTotal) {
    return {
      ok: false,
      message: `Rango inválido: solo existen etiquetas del 1 al ${formatNumber(envaseTotal)} para este lote. No se puede reimprimir el envase ${formatNumber(Math.max(from, to))}.`,
    };
  }
  if (from > to) {
    return { ok: false, message: "Desde no puede ser mayor que Hasta." };
  }
  return { ok: true, from, to };
}
