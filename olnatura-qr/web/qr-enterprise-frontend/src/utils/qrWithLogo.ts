import * as QRCode from "qrcode";

export type QrWithLogoOptions = {
  errorCorrectionLevel?: "L" | "M" | "Q" | "H";
  margin?: number;
  width?: number;
  color?: { dark: string; light: string };

  logoUrl?: string;  
  logoSizeRatio?: number;
  badgeSizeRatio?: number; 
  badgeRadiusRatio?: number; 
  badgeBorderWidth?: number; 
  badgeShadow?: boolean;   
  debug?: boolean;         
};

const DEFAULTS: Required<Omit<QrWithLogoOptions, "logoUrl">> = {
  errorCorrectionLevel: "H",
  margin: 4,
  width: 800,
  color: { dark: "#0B0B0B", light: "#FFFFFF" },

  logoSizeRatio: 0.20,
  badgeSizeRatio: 0.26,
  badgeRadiusRatio: 0.18,
  badgeBorderWidth: 6,
  badgeShadow: true,
  debug: false,
};

export async function generateQrPlain(
  payload: string,
  opts: { width?: number; margin?: number; errorCorrectionLevel?: "L" | "M" | "Q" | "H" } = {}
): Promise<string> {
  const { width = 220, margin = 2, errorCorrectionLevel = "M" } = opts;
  return QRCode.toDataURL(payload, {
    errorCorrectionLevel,
    margin,
    width,
    color: { dark: "#0B0B0B", light: "#FFFFFF" },
  });
}

export async function generateQrWithLogo(
  payload: string,
  opts: QrWithLogoOptions = {}
): Promise<string> {
  const merged = { ...DEFAULTS, ...opts };
  const { errorCorrectionLevel, margin, width, color } = merged;

  const qrDataUrl = await QRCode.toDataURL(payload, {
    errorCorrectionLevel,
    margin,
    width,
    color,
  });

  const preferredLogo = opts.logoUrl ?? "/logo-olnatura.png";
  const fallbackLogo = "/vite.svg";

  try {
    return await drawLogoOverlay(qrDataUrl, preferredLogo, fallbackLogo, merged);
  } catch {
    return qrDataUrl;
  }
}

async function drawLogoOverlay(
  qrDataUrl: string,
  preferredLogo: string,
  fallbackLogo: string,
  opts: Required<Omit<QrWithLogoOptions, "logoUrl">>
): Promise<string> {
  const qrImg = await loadImage(qrDataUrl);
  let logoImg: HTMLImageElement;
  try {
    logoImg = await loadImage(preferredLogo);
  } catch {
    logoImg = await loadImage(fallbackLogo);
  }

  const size = qrImg.naturalWidth || qrImg.width;
  if (!size) throw new Error("QR image invalid size");

  const canvas = document.createElement("canvas");
  canvas.width = size;
  canvas.height = size;

  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("Canvas not supported");

  ctx.drawImage(qrImg, 0, 0, size, size);

  const badgeSize = Math.round(size * opts.badgeSizeRatio);
  const logoSize = Math.round(size * opts.logoSizeRatio);

  const cx = size / 2;
  const cy = size / 2;

  const bx = Math.round(cx - badgeSize / 2);
  const by = Math.round(cy - badgeSize / 2);

  if (opts.badgeShadow) {
    ctx.save();
    ctx.shadowColor = "rgba(0,0,0,0.20)";
    ctx.shadowBlur = 14;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 6;
    drawRoundedRect(ctx, bx, by, badgeSize, badgeSize, Math.round(badgeSize * opts.badgeRadiusRatio));
    ctx.fillStyle = "#FFFFFF";
    ctx.fill();
    ctx.restore();
  } else {
    drawRoundedRect(ctx, bx, by, badgeSize, badgeSize, Math.round(badgeSize * opts.badgeRadiusRatio));
    ctx.fillStyle = "#FFFFFF";
    ctx.fill();
  }

  drawRoundedRect(ctx, bx, by, badgeSize, badgeSize, Math.round(badgeSize * opts.badgeRadiusRatio));
  ctx.lineWidth = opts.badgeBorderWidth;
  ctx.strokeStyle = "#4d8a52";
  ctx.stroke();

  const lx = Math.round(cx - logoSize / 2);
  const ly = Math.round(cy - logoSize / 2);

  ctx.drawImage(logoImg, lx, ly, logoSize, logoSize);

  return canvas.toDataURL("image/png");
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();

    img.onload = () => {
      const anyImg = img as any;
      if (typeof anyImg.decode === "function") {
        anyImg.decode().then(() => resolve(img)).catch(() => resolve(img));
      } else {
        resolve(img);
      }
    };

    img.onerror = () => reject(new Error(`Image load failed: ${src}`));
    img.src = src;
  });
}

function drawRoundedRect(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  r: number
) {
  const radius = Math.max(0, Math.min(r, Math.floor(Math.min(w, h) / 2)));
  ctx.beginPath();
  ctx.moveTo(x + radius, y);
  ctx.lineTo(x + w - radius, y);
  ctx.quadraticCurveTo(x + w, y, x + w, y + radius);
  ctx.lineTo(x + w, y + h - radius);
  ctx.quadraticCurveTo(x + w, y + h, x + w - radius, y + h);
  ctx.lineTo(x + radius, y + h);
  ctx.quadraticCurveTo(x, y + h, x, y + h - radius);
  ctx.lineTo(x, y + radius);
  ctx.quadraticCurveTo(x, y, x + radius, y);
  ctx.closePath();
}