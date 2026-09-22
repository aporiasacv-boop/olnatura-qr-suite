export const LABEL_DOCUMENT_CODE = "AL-001-E02/06";
const LEGACY_DOCUMENT_CODE = "AL-001-E02/04";

export function resolveLabelDocumentCode(code?: string | null): string {
  const raw = String(code ?? "").trim();
  if (!raw) return LABEL_DOCUMENT_CODE;
  if (raw.startsWith(LEGACY_DOCUMENT_CODE)) {
    return LABEL_DOCUMENT_CODE + raw.slice(LEGACY_DOCUMENT_CODE.length);
  }
  return raw;
}
