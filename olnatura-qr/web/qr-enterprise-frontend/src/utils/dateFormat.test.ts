import assert from "node:assert/strict";
import { test } from "node:test";
import { formatDateDDMMYYYY, formatDateLabelDDMMMYY } from "./dateFormat.ts";
import { resolveLabelDocumentCode } from "./labelDocumentCode.ts";

test("etiquetas usan DD/MMM/YY sin cambiar el formato general", () => {
  assert.equal(formatDateLabelDDMMMYY("2026-09-10"), "10/SEP/26");
  assert.equal(formatDateLabelDDMMMYY("10/09/2026"), "10/SEP/26");
  assert.equal(formatDateDDMMYYYY("10/09/2026"), "10/09/2026");
});

test("referencia documental actualiza solo el prefijo", () => {
  assert.equal(resolveLabelDocumentCode(null), "AL-001-E02/06");
  assert.equal(resolveLabelDocumentCode("AL-001-E02/04"), "AL-001-E02/06");
  assert.equal(
    resolveLabelDocumentCode("AL-001-E02/04 Propiedad"),
    "AL-001-E02/06 Propiedad"
  );
});
