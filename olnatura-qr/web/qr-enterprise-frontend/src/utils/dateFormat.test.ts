import assert from "node:assert/strict";
import { test } from "node:test";
import { etiquetaFechaCajas, fechaTipoEtiqueta, formatDateDDMMYYYY, formatDateLabelDDMMMYY, storedDateText } from "./dateFormat.ts";
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
  assert.equal(storedDateText("2028-08-21"), "2028-08-21");
  assert.equal(storedDateText([2028, 8, 21]), "2028-08-21");
  assert.equal(formatDateDDMMYYYY(storedDateText([2028, 8, 21])), "21/08/2028");
  assert.equal(formatDateLabelDDMMMYY(storedDateText([2028, 8, 21])), "21/AGO/28");
});

test("consulta muestra solo reanálisis o solo caducidad", () => {
  assert.equal(fechaTipoEtiqueta(null, "2028-08-21"), "REANALISIS");
  assert.equal(fechaTipoEtiqueta("2028-08-21", null), "CADUCIDAD");
  assert.equal(fechaTipoEtiqueta("2028-08-21", "2029-01-01"), "REANALISIS");
  assert.equal(fechaTipoEtiqueta("", "  "), null);
});

test("la etiqueta muestra N/A en la fecha que no aplica", () => {
  assert.deepEqual(etiquetaFechaCajas(null, "2028-08-21"), {
    caducidad: "N/A",
    reanalisis: "21/AGO/28",
  });
  assert.deepEqual(etiquetaFechaCajas("2028-08-21", null), {
    caducidad: "21/AGO/28",
    reanalisis: "N/A",
  });
  assert.deepEqual(etiquetaFechaCajas("2028-08-20", "2028-08-21"), {
    caducidad: "N/A",
    reanalisis: "21/AGO/28",
  });
  assert.deepEqual(etiquetaFechaCajas(null, null), {
    caducidad: "N/A",
    reanalisis: "N/A",
  });
});

test("referencia documental actualiza solo el prefijo", () => {
  assert.equal(resolveLabelDocumentCode(null), "AL-001-E02/06");
  assert.equal(resolveLabelDocumentCode("AL-001-E02/04"), "AL-001-E02/06");
  assert.equal(
    resolveLabelDocumentCode("AL-001-E02/04 Propiedad"),
    "AL-001-E02/06 Propiedad"
  );
});
