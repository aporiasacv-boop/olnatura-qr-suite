/**
 * Genera un QR con logo centrado (overlay).
 * SRP: solo la generación del QR con logo; el nivel H permite ~30% de datos para el logo.
 */

import * as QRCode from "qrcode";

export type QrWithLogoOptions = {
  errorCorrectionLevel?: "L" | "M" | "Q" | "H";
  margin?: number;
  width?: number;
  color?: { dark: string; light: string };
  logoUrl?: string;
  logoSizeRatio?: number; // 0.2 = 20% del ancho del QR
};

const DEFAULTS: Required<Omit<QrWithLogoOptions, "logoUrl">> = {
  errorCorrectionLevel: "H",
  margin: 4,
  width: 800,
  color: { dark: "#0B0B0B", light: "#FFFFFF" },
  logoSizeRatio: 0.22,
};

/**
 * Genera un QR como data URL (PNG) con logo centrado.
 * Si logoUrl falla, genera QR sin logo.
 */
export async function generateQrWithLogo(
  payload: string,
  opts: QrWithLogoOptions = {}
): Promise<string> {
  const { errorCorrectionLevel, margin, width, color, logoSizeRatio } = {
    ...DEFAULTS,
    ...opts,
  };

  const dataUrl = await QRCode.toDataURL(payload, {
    errorCorrectionLevel,
    margin,
    width,
    color,
  });

  const logoUrl = opts.logoUrl ?? "/logo-olnatura.png";
  try {
    return await drawLogoOverlay(dataUrl, logoUrl, logoSizeRatio);
  } catch {
    return dataUrl;
  }
}

function drawLogoOverlay(
  qrDataUrl: string,
  logoUrl: string,
  logoSizeRatio: number
): Promise<string> {
  return new Promise((resolve, reject) => {
    const canvas = document.createElement("canvas");
    const ctx = canvas.getContext("2d");
    if (!ctx) {
      reject(new Error("Canvas not supported"));
      return;
    }

    const qrImg = new Image();
    qrImg.crossOrigin = "anonymous";

    qrImg.onload = () => {
      const size = qrImg.width;
      canvas.width = size;
      canvas.height = size;
      ctx.drawImage(qrImg, 0, 0);

      const logoImg = new Image();
      logoImg.crossOrigin = "anonymous";

      logoImg.onload = () => {
        const logoSize = Math.round(size * logoSizeRatio);
        const x = (size - logoSize) / 2;
        const y = (size - logoSize) / 2;

        // Fondo blanco bajo el logo para mejor contraste
        ctx.fillStyle = "#FFFFFF";
        const pad = 4;
        ctx.fillRect(x - pad, y - pad, logoSize + pad * 2, logoSize + pad * 2);

        ctx.drawImage(logoImg, x, y, logoSize, logoSize);
        resolve(canvas.toDataURL("image/png"));
      };

      logoImg.onerror = () => reject(new Error("Logo load failed"));
      logoImg.src = logoUrl;
    };

    qrImg.onerror = () => reject(new Error("QR image load failed"));
    qrImg.src = qrDataUrl;
  });
}
