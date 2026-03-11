package com.company.olnaturaqr.support.zpl;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * Converts a PNG image (e.g. QR with logo) to ZPL ^GFA graphic format.
 * Output is 1-bit monochrome, hex-encoded for Zebra printers.
 */
public final class ZplGraphicUtil {

    private ZplGraphicUtil() {}

    /**
     * Convert base64 PNG (with optional data URL prefix) to ZPL ^GFA command.
     *
     * @param base64OrDataUrl Base64 string, optionally with "data:image/png;base64," prefix
     * @param maxSize         Max width/height to scale to (keeps aspect ratio)
     * @return ZPL graphic string e.g. "^GFA,5000,25,25,:H08...:F8" or empty on error
     */
    public static String toGfa(String base64OrDataUrl, int maxSize) {
        if (base64OrDataUrl == null || base64OrDataUrl.isBlank()) return "";

        String base64 = base64OrDataUrl;
        int idx = base64.indexOf("base64,");
        if (idx >= 0) base64 = base64.substring(idx + 7).trim();

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            return "";
        }
        if (bytes == null || bytes.length == 0) return "";

        BufferedImage img;
        try {
            img = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            return "";
        }
        if (img == null) return "";

        return toGfa(img, maxSize);
    }

    public static String toGfa(BufferedImage img, int maxSize) {
        if (img == null) return "";

        int w = img.getWidth();
        int h = img.getHeight();
        if (w <= 0 || h <= 0) return "";

        if (w > maxSize || h > maxSize) {
            double scale = Math.min((double) maxSize / w, (double) maxSize / h);
            int nw = (int) Math.round(w * scale);
            int nh = (int) Math.round(h * scale);
            BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(img, 0, 0, nw, nh, null);
            g2.dispose();
            img = scaled;
            w = nw;
            h = nh;
        }

        int bytesPerRow = (w + 7) / 8;
        int totalBytes = bytesPerRow * h;

        byte[] mono = new byte[totalBytes];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int gray = ((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF);
                boolean black = gray < 384;
                if (black) {
                    int byteIdx = y * bytesPerRow + (x / 8);
                    int bit = 7 - (x % 8);
                    mono[byteIdx] |= (byte) (1 << bit);
                }
            }
        }

        StringBuilder hex = new StringBuilder(totalBytes * 2);
        for (byte b : mono) {
            hex.append(String.format("%02X", b & 0xFF));
        }

        return String.format("^GFA,%d,%d,%d,%s", totalBytes, bytesPerRow, bytesPerRow, hex.toString());
    }
}
