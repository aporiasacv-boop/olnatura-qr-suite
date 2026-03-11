/**
 * Export LabelPreview DOM to PNG using html-to-image.
 * Ensures the exported PNG matches the preview exactly.
 */

import { toPng } from "html-to-image";

const DEFAULT_PIXEL_RATIO = 2;

/**
 * Export a label preview element to PNG data URL.
 * Uses higher pixel ratio for print quality.
 */
export async function exportLabelPreviewToPng(
  element: HTMLElement,
  options?: { pixelRatio?: number }
): Promise<string> {
  const pixelRatio = options?.pixelRatio ?? DEFAULT_PIXEL_RATIO;

  return toPng(element, {
    pixelRatio,
    cacheBust: true,
    backgroundColor: "#ffffff",
    style: {
      margin: "0",
    },
  });
}
