from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import numpy as np

SOURCE = Path(r"C:\Users\BecarioQR\AppData\Roaming\Cursor\User\workspaceStorage\empty-window\images\Olnatura-7e253b01-cfd5-48d6-83a7-c2f7502e6465.png")
RES_ROOT = Path(r"C:\Users\BecarioQR\OneDrive - OLNATURA, S.A. DE CV\Escritorio\olnatura-qr-suite\qr-suite\olnatura-qr-suite\OlnaturaQR\app\src\main\res")
WEB_LOGO = Path(r"C:\Users\BecarioQR\OneDrive - OLNATURA, S.A. DE CV\Escritorio\olnatura-qr-suite\qr-suite\olnatura-qr-suite\olnatura-qr\web\qr-enterprise-frontend\public\logo-olnatura.png")
PREVIEW_DIR = Path(r"C:\Users\BecarioQR\OneDrive - OLNATURA, S.A. DE CV\Escritorio\olnatura-qr-suite\qr-suite\olnatura-qr-suite\OlnaturaQR\icon-gen-preview")

BG = (253, 251, 235)
PADDING_RATIO = 0.15
DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

def symbol_bbox(rgb_image: Image.Image):
    arr = np.array(rgb_image.convert("RGB"))
    bg_like = (arr[:, :, 0] > 235) & (arr[:, :, 1] > 235) & (arr[:, :, 2] > 220)
    ink = ~bg_like
    ys, xs = np.where(ink)
    col = ink.sum(axis=0)
    thresh = col.max() * 0.15
    above = np.where(col > thresh)[0]
    i = 0
    while i < len(above) - 1 and above[i + 1] - above[i] <= 2:
        i += 1
    first_cluster_end = above[i]
    best_cut = None
    w = arr.shape[1]
    for cut in range(first_cluster_end, min(w // 2, xs.max())):
        if col[cut : cut + 12].max() < thresh * 0.3:
            best_cut = cut
            break
    if best_cut is None:
        best_cut = int(w * 0.32)
    sym = ink[:, :best_cut]
    ys, xs = np.where(sym)
    return int(xs.min()), int(ys.min()), int(xs.max()), int(ys.max())

def extract_mark(source: Image.Image) -> Image.Image:
    src = source.convert("RGBA")
    x0, y0, x1, y1 = symbol_bbox(src)
    pad_x = int((x1 - x0) * 0.06)
    pad_y = int((y1 - y0) * 0.06)
    x0 = max(0, x0 - pad_x)
    y0 = max(0, y0 - pad_y)
    x1 = min(src.width - 1, x1 + pad_x)
    y1 = min(src.height - 1, y1 + pad_y)
    crop = src.crop((x0, y0, x1 + 1, y1 + 1))
    px = crop.load()
    cw, ch = crop.size
    for y in range(ch):
        for x in range(cw):
            r, g, b, a = px[x, y]
            if r > 235 and g > 235 and b > 220:
                px[x, y] = (r, g, b, 0)
    return crop

def compose_square_icon(mark: Image.Image, size: int) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), BG + (255,))
    pad = int(size * PADDING_RATIO)
    inner = size - 2 * pad
    mark_rgb = mark.copy()
    mark_rgb.thumbnail((inner, inner), Image.Resampling.LANCZOS)
    mw, mh = mark_rgb.size
    ox = (size - mw) // 2
    oy = (size - mh) // 2
    canvas.alpha_composite(mark_rgb, (ox, oy))
    return canvas.convert("RGB")

def compose_round_icon(square_rgb: Image.Image) -> Image.Image:
    size = square_rgb.width
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    inset = max(1, size // 64)
    draw.ellipse((inset, inset, size - inset - 1, size - inset - 1), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(radius=max(0.5, size / 192)))
    out = Image.new("RGBA", (size, size), BG + (255,))
    out.paste(square_rgb, (0, 0))
    out.putalpha(mask)
    bg = Image.new("RGBA", (size, size), BG + (255,))
    bg.alpha_composite(out)
    return bg.convert("RGB")

if __name__ == "__main__":
    source = Image.open(SOURCE)
    mark = extract_mark(source)
    PREVIEW_DIR.mkdir(parents=True, exist_ok=True)
    mark.save(PREVIEW_DIR / "mark-symbol-crop.png")
    for folder, px in DENSITIES.items():
        out_dir = RES_ROOT / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        square = compose_square_icon(mark, px)
        round_icon = compose_round_icon(square)
        square.save(out_dir / "ic_launcher.webp", format="WEBP", quality=92, method=6)
        round_icon.save(out_dir / "ic_launcher_round.webp", format="WEBP", quality=92, method=6)
        if folder == "mipmap-xxxhdpi":
            square.save(PREVIEW_DIR / "ic_launcher_192.png")
            round_icon.save(PREVIEW_DIR / "ic_launcher_round_192.png")
    source.convert("RGB").save(WEB_LOGO, format="PNG", optimize=True)
