export const MAX_CANTIDADES_MENORES = 4;

export type LabelCantidades = {
  restosEnabled?: boolean | null;
  envaseTotal?: number | string | null;
  cantidadPorEnvase?: string | null;
  cantidadResto?: string | null;
  restosCantidades?: string[] | null;
} | null | undefined;

export function isRestosEnabled(label: LabelCantidades): boolean {
  return cantidadesMenoresOf(label).length > 0;
}

export function cantidadesMenoresOf(label: LabelCantidades): string[] {
  const list = (label?.restosCantidades ?? [])
    .map((v) => String(v ?? "").trim())
    .filter((v) => v.length > 0);
  if (list.length > 0) return list.slice(0, MAX_CANTIDADES_MENORES);
  if (label?.restosEnabled) {
    const legacy = String(label.cantidadResto ?? "").trim();
    if (legacy) return [legacy];
  }
  return [];
}

export function isCantidadMenorEnvase(label: LabelCantidades, envaseNum: number): boolean {
  const list = cantidadesMenoresOf(label);
  if (list.length === 0) return false;
  const total = Number(label?.envaseTotal);
  if (!Number.isFinite(total)) return false;
  const first = total - list.length + 1;
  return envaseNum >= first && envaseNum <= total;
}

export function cantidadForEnvase(label: LabelCantidades, envaseNum: number): string {
  const list = cantidadesMenoresOf(label);
  if (list.length > 0) {
    const total = Number(label?.envaseTotal);
    const first = total - list.length + 1;
    if (Number.isFinite(total) && envaseNum >= first && envaseNum <= total) {
      return list[envaseNum - first];
    }
  }
  const std = String(label?.cantidadPorEnvase ?? "").trim();
  return std || "N/A";
}

export function parseCantidad(raw: string): number | null {
  const n = String(raw ?? "").trim().replace(/\s/g, "").replace(",", ".");
  const m = n.match(/^[+-]?(\d+(\.\d+)?)/);
  if (!m) return null;
  const v = Number(m[0]);
  if (!Number.isFinite(v) || v <= 0) return null;
  return v;
}

export function cantidadesMenoresOk(
  cantidades: string[],
  cantidadPorEnvase: string,
  envaseTotal: number
): boolean {
  const filled = (cantidades ?? []).map((v) => String(v ?? "").trim()).filter((v) => v.length > 0);
  if (filled.length === 0) return true;
  if (filled.length > MAX_CANTIDADES_MENORES) return false;
  if (envaseTotal < filled.length) return false;
  const std = parseCantidad(cantidadPorEnvase);
  if (std == null) return false;
  return filled.every((raw) => {
    const q = parseCantidad(raw);
    return q != null && q > 0 && q < std;
  });
}

export function envaseQuantities(
  envaseTotal: number,
  cantidadPorEnvase: string,
  cantidadesMenores: string[]
): string[] {
  const total = Math.trunc(Number(envaseTotal));
  if (!Number.isFinite(total) || total < 1) return [];
  const std = String(cantidadPorEnvase ?? "").trim() || "—";
  const menores = (cantidadesMenores ?? [])
    .map((v) => String(v ?? "").trim())
    .filter((v) => v.length > 0)
    .slice(0, Math.min(MAX_CANTIDADES_MENORES, total));
  const firstResto = total - menores.length + 1;
  return Array.from({ length: total }, (_, i) => {
    const envase = i + 1;
    if (menores.length > 0 && envase >= firstResto) {
      return menores[envase - firstResto];
    }
    return std;
  });
}

export function envaseDistribution(
  envaseTotal: number,
  cantidadPorEnvase: string,
  cantidadesMenores: string[]
): string {
  return envaseQuantities(envaseTotal, cantidadPorEnvase, cantidadesMenores)
    .map((q) => `${q} Pz`)
    .join(" / ");
}

export function cantidadTotalOf(label: LabelCantidades): string {
  const total = Number(label?.envaseTotal);
  const qs = envaseQuantities(total, String(label?.cantidadPorEnvase ?? ""), cantidadesMenoresOf(label));
  if (qs.length === 0) return "0";
  let sum = 0;
  let parsed = 0;
  for (const raw of qs) {
    const n = parseCantidad(raw);
    if (n == null) continue;
    sum += n;
    parsed += 1;
  }
  if (parsed === 0) return String(qs.length);
  return Number.isInteger(sum) ? String(sum) : String(sum);
}
