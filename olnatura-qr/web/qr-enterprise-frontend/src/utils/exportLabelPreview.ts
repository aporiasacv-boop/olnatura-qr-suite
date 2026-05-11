

import { toPng } from "html-to-image";


const DEFAULT_PIXEL_RATIO = 3;


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
