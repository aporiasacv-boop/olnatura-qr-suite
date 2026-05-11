import { API_BASE } from "../api/client";

/**
 * Descarga ZPL vía GET (mismo contrato que consulta por lote).
 * Evita POST con body grande (data URL del QR) que puede fallar en proxies o límites de tamaño.
 */
export async function downloadLabelZplFile(opts: {
  labelIdOrLote: string;
  totalEnvases: number;
  printFrom?: number;
  printTo?: number;
}): Promise<void> {
  const key = (opts.labelIdOrLote ?? "").trim();
  if (!key) throw new Error("Identificador de etiqueta vacío");

  const total = Math.max(1, opts.totalEnvases);
  const from = opts.printFrom ?? 1;
  const to = opts.printTo ?? total;

  const base = API_BASE.replace(/\/+$/, "");
  const params = new URLSearchParams();
  params.set("total", String(total));
  params.set("from", String(from));
  params.set("to", String(to));
  const qs = params.toString();
  const url = `${base}/api/v1/label/${encodeURIComponent(key)}/zpl${qs ? `?${qs}` : ""}`;

  const res = await fetch(url, { method: "GET", credentials: "include" });
  if (!res.ok) {
    const t = await res.text().catch(() => "");
    throw new Error(t || `Error ${res.status}`);
  }

  const text = await res.text();
  const blob = new Blob([text], { type: "text/plain;charset=utf-8" });
  const href = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = href;

  const cd = res.headers.get("Content-Disposition");
  let filename = `etiqueta-${key.replace(/[\s/\\]+/g, "_")}.zpl`;
  if (cd) {
    const m = cd.match(/filename="?([^";\n]+)"?/);
    if (m?.[1]) filename = m[1].trim();
  } else if (from !== to) {
    filename = `etiqueta-${key.replace(/[\s/\\]+/g, "_")}-del-${from}-al-${to}.zpl`;
  }
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.setTimeout(() => URL.revokeObjectURL(href), 2500);
}
