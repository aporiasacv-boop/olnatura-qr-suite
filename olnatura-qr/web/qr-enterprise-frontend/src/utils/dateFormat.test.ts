import assert from "node:assert/strict";
import { test } from "node:test";
import { formatDateDDMMYYYY, formatDateLabelDDMMMYY } from "./dateFormat.ts";
import { resolveLabelDocumentCode } from "./labelDocumentCode.ts";

test("etiquetas usan DD/MMM/YY sin cambiar el formato general", () => {
  assert.equal(formatDateLabelDDMMMYY("2026-09-10"), "10/SEP/26");
  assert.equal(formatDateLabelDDMMMYY("10/09/2026"), "10/SEP/26");
  assert.equal(formatDateDDMMYYYY("10/09/2026"), "10/09/2026");
});

test("ISO de solo fecha conserva el día calendario sin desfase UTC", () => {
  assert.equal(formatDateDDMMYYYY("2028-08-21"), "21/08/2028");
  assert.equal(formatDateLabelDDMMMYY("2028-08-21"), "21/AGO/28");
  assert.equal(formatDateDDMMYYYY("2028-08-21T00:00:00.000Z"), "21/08/2028");
  assert.equal(formatDateDDMMYYYY("2026-09-01"), "01/09/2026");
  assert.equal(formatDateLabelDDMMMYY("2026-09-01"), "01/SEP/26");
});

test("referencia documental actualiza solo el prefijo", () => {
  assert.equal(resolveLabelDocumentCode(null), "AL-001-E02/06");
  assert.equal(resolveLabelDocumentCode("AL-001-E02/04"), "AL-001-E02/06");
  assert.equal(
    resolveLabelDocumentCode("AL-001-E02/04 Propiedad"),
    "AL-001-E02/06 Propiedad"
  );
});
