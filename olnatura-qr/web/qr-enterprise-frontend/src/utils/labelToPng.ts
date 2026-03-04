/**
 * Renderiza una etiqueta imprimible (campos estáticos + QR) a PNG.
 * NO incluye estatus (es dinámico).
 */

export type FechaTipo = "CADUCIDAD" | "REANALISIS";

export type LabelData = {
  tipoMaterial?: string;
  nombre?: string;
  codigo?: string;
  lote?: string;
  fechaEntrada?: string;
  /** Tipo de fecha a mostrar: solo uno visible/imprimible */
  fechaTipo?: FechaTipo;
  /** Valor de la fecha (usado según fechaTipo) */
  fechaValor?: string;
  /** @deprecated Legacy: si no hay fechaTipo/fechaValor, se usa para inferir */
  caducidad?: string;
  /** @deprecated Legacy: si no hay fechaTipo/fechaValor, se usa para inferir */
  reanalisis?: string;
  envaseNum?: number;
  envaseTotal?: number;
};

function resolveFechaRow(label: LabelData): { fechaLabel: string; fechaValor: string } {
  if (label.fechaTipo === "CADUCIDAD") {
    return { fechaLabel: "Caducidad", fechaValor: label.fechaValor ?? label.caducidad ?? "" };
  }
  if (label.fechaTipo === "REANALISIS") {
    return { fechaLabel: "Reanálisis", fechaValor: label.fechaValor ?? label.reanalisis ?? "" };
  }
  if (label.caducidad?.trim()) {
    return { fechaLabel: "Caducidad", fechaValor: label.caducidad };
  }
  if (label.reanalisis?.trim()) {
    return { fechaLabel: "Reanálisis", fechaValor: label.reanalisis };
  }
  return { fechaLabel: "Caducidad", fechaValor: "" };
}

function row(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  label: string,
  value: string
) {
  ctx.fillStyle = "#6B7280";
  ctx.font = "12px system-ui, sans-serif";
  ctx.fillText(label, x, y);
  ctx.fillStyle = "#111827";
  ctx.font = "bold 14px system-ui, sans-serif";
  ctx.fillText(value || "—", x, y + 18);
}

/**
 * Genera PNG de etiqueta (solo datos estáticos, sin estatus).
 */
export async function renderLabelToPng(
  label: LabelData,
  qrDataUrl: string
): Promise<string> {
  const qrImg = await loadImage(qrDataUrl);
  const qrSize = 200;
  const pad = 24;
  const lineH = 32;
  const rowCount = 7;
  const w = 500;
  const h = pad * 2 + rowCount * lineH + qrSize + pad;

  const canvas = document.createElement("canvas");
  canvas.width = w;
  canvas.height = h;
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("Canvas not supported");

  // Fondo blanco + borde verde
  ctx.fillStyle = "#FFFFFF";
  ctx.fillRect(0, 0, w, h);
  ctx.strokeStyle = "#45a350";
  ctx.lineWidth = 3;
  ctx.strokeRect(2, 2, w - 4, h - 4);

  let y = pad + 16;
  const x = pad;

  row(ctx, x, y, "Tipo material", String(label.tipoMaterial ?? ""));
  y += lineH;
  row(ctx, x, y, "Nombre", String(label.nombre ?? ""));
  y += lineH;
  row(ctx, x, y, "Código", String(label.codigo ?? ""));
  y += lineH;
  row(ctx, x, y, "Lote", String(label.lote ?? ""));
  y += lineH;
  row(ctx, x, y, "Fecha de entrada", String(label.fechaEntrada ?? ""));
  y += lineH;
  const { fechaLabel, fechaValor } = resolveFechaRow(label);
  row(ctx, x, y, fechaLabel, String(fechaValor ?? "—"));
  y += lineH;
  row(ctx, x, y, "Envase", `${label.envaseNum ?? "—"} / ${label.envaseTotal ?? "—"}`);
  y += lineH + pad;

  // QR centrado abajo
  const qrX = (w - qrSize) / 2;
  ctx.drawImage(qrImg, qrX, y, qrSize, qrSize);

  return canvas.toDataURL("image/png");
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error(`Image load failed: ${src}`));
    img.src = src;
  });
}
