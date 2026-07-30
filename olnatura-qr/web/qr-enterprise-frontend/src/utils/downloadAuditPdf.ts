import { API_BASE } from "../api/client";

export async function downloadAuditPdf(
  loteInput: string,
  onError: (msg: string) => void
): Promise<void> {
  const lote = (loteInput ?? "").trim();
  if (!lote) return;

  const base = API_BASE.replace(/\/+$/, "");
  const url = `${base}/api/v1/audit/${encodeURIComponent(lote)}/pdf`;

  try {
    const res = await fetch(url, { method: "GET", credentials: "include" });
    if (!res.ok) {
      onError(res.status === 404 ? "Lote no encontrado." : "No se pudo descargar el PDF.");
      return;
    }
    const blob = await res.blob();
    const href = URL.createObjectURL(blob);
    const a = document.createElement("a");
    const safeLote = (lote || "lote").replace(/[\s/\\]+/g, "_");
    a.href = href;
    a.download = `trazabilidad-${safeLote}.pdf`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(href);
  } catch {
    onError("Error al descargar. Verifica la conexión.");
  }
}
