import { API_BASE } from "../api/client";

export async function downloadUsersPdf(
  onError: (msg: string) => void
): Promise<string | null> {
  const base = API_BASE.replace(/\/+$/, "");
  const url = `${base}/api/v1/admin/users/pdf`;

  try {
    const res = await fetch(url, { method: "GET", credentials: "include" });
    if (!res.ok) {
      onError(res.status === 403 ? "Sin permiso (solo ADMIN)." : "No se pudo generar el PDF.");
      return null;
    }
    const blob = await res.blob();
    const href = URL.createObjectURL(blob);
    const a = document.createElement("a");
    const cd = res.headers.get("Content-Disposition") || "";
    const match = /filename="?([^"]+)"?/i.exec(cd);
    a.href = href;
    a.download = match?.[1] || "usuarios-olnatura-qr.pdf";
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(href);
    return a.download;
  } catch {
    onError("Error al descargar. Verifica la conexión.");
    return null;
  }
}
