/**
 * Relación almacén/código Dynamics → categoría de aprobación Olnatura.
 *
 * MPM / MPS → Materia Prima (fijo)
 * MEM / MES → Empaque (usuario elige primario o secundario)
 */
export type DynamicsSiteFamily = "MATERIA_PRIMA" | "EMPAQUE" | "DESCONOCIDO";

export type MaterialCategory = "MATERIA_PRIMA" | "EMPAQUE_PRIMARIO" | "EMPAQUE_SECUNDARIO";

const MATERIA_PRIMA_SITES = new Set(["MPM", "MPS"]);
const EMPAQUE_SITES = new Set(["MEM", "MES"]);

/** Extrae código de sitio del almacén Dynamics o del lote (ej. 260713-MEM0003662). */
export function extractDynamicsSiteCode(almacen?: string | null, lote?: string | null): string | null {
  const fromAlmacen = (almacen ?? "").trim().toUpperCase();
  if (/^(MPM|MPS|MEM|MES)$/.test(fromAlmacen)) return fromAlmacen;

  const fromLote = (lote ?? "").trim().toUpperCase();
  const m = fromLote.match(/(?:^|[^A-Z])(MPM|MPS|MEM|MES)(?=\d|$)/);
  if (m) return m[1];

  // Prefijo típico tras el guión: 260713-MEM0003662
  const m2 = fromLote.match(/-([A-Z]{3})\d/);
  if (m2 && /^(MPM|MPS|MEM|MES)$/.test(m2[1])) return m2[1];

  return null;
}

export function dynamicsSiteFamily(almacen?: string | null, lote?: string | null): DynamicsSiteFamily {
  const code = extractDynamicsSiteCode(almacen, lote);
  if (!code) return "DESCONOCIDO";
  if (MATERIA_PRIMA_SITES.has(code)) return "MATERIA_PRIMA";
  if (EMPAQUE_SITES.has(code)) return "EMPAQUE";
  return "DESCONOCIDO";
}

export function dynamicsSiteLabel(code: string | null): string {
  switch (code) {
    case "MPM":
      return "MPM — Materia prima medicamentos";
    case "MPS":
      return "MPS — Materia prima suplementos";
    case "MEM":
      return "MEM — Material de empaque medicamentos";
    case "MES":
      return "MES — Material de empaque suplementos";
    default:
      return code ?? "—";
  }
}

export function materialCategoryDisplay(value: string): string {
  switch (value) {
    case "MATERIA_PRIMA":
      return "Materia Prima";
    case "EMPAQUE_PRIMARIO":
      return "Material de Empaque Primario";
    case "EMPAQUE_SECUNDARIO":
      return "Material de Empaque Secundario";
    default:
      return "";
  }
}
