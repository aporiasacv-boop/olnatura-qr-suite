import assert from "node:assert/strict";
import { test } from "node:test";
import {
  MAX_CANTIDADES_MENORES,
  cantidadForEnvase,
  cantidadTotalOf,
  cantidadesMenoresOf,
  cantidadesMenoresOk,
  envaseDistribution,
  envaseQuantities,
} from "./envaseRestos.ts";

test("A: lote sin cantidades menores", () => {
  assert.equal(MAX_CANTIDADES_MENORES, 4);
  assert.deepEqual(cantidadesMenoresOf({ restosCantidades: [] }), []);
  assert.equal(cantidadesMenoresOk([], "100", 5), true);
  assert.equal(cantidadForEnvase({ cantidadPorEnvase: "100", envaseTotal: 5, restosCantidades: [] }, 1), "100");
  assert.equal(cantidadForEnvase({ cantidadPorEnvase: "100", envaseTotal: 5, restosCantidades: [] }, 5), "100");
});

test("B: una cantidad menor independiente", () => {
  const label = { cantidadPorEnvase: "100", envaseTotal: 5, restosCantidades: ["5"] };
  assert.equal(cantidadesMenoresOk(["5"], "100", 5), true);
  assert.equal(cantidadForEnvase(label, 4), "100");
  assert.equal(cantidadForEnvase(label, 5), "5");
  assert.equal(cantidadForEnvase(label, 5).toUpperCase().includes("RESTOS"), false);
});

test("C: dos cantidades independientes", () => {
  const label = { cantidadPorEnvase: "100", envaseTotal: 5, restosCantidades: ["5", "8"] };
  assert.equal(cantidadForEnvase(label, 3), "100");
  assert.equal(cantidadForEnvase(label, 4), "5");
  assert.equal(cantidadForEnvase(label, 5), "8");
});

test("D: tres cantidades independientes", () => {
  const label = { cantidadPorEnvase: "100", envaseTotal: 6, restosCantidades: ["5", "8", "3"] };
  assert.equal(cantidadForEnvase(label, 3), "100");
  assert.equal(cantidadForEnvase(label, 4), "5");
  assert.equal(cantidadForEnvase(label, 5), "8");
  assert.equal(cantidadForEnvase(label, 6), "3");
});

test("E: cuatro cantidades independientes y no una quinta", () => {
  const qs = ["5", "8", "3", "6"];
  const label = { cantidadPorEnvase: "100", envaseTotal: 6, restosCantidades: qs };
  assert.equal(cantidadesMenoresOk(qs, "100", 6), true);
  assert.equal(cantidadesMenoresOk([...qs, "2"], "100", 8), false);
  assert.equal(cantidadForEnvase(label, 2), "100");
  assert.equal(cantidadForEnvase(label, 3), "5");
  assert.equal(cantidadForEnvase(label, 4), "8");
  assert.equal(cantidadForEnvase(label, 5), "3");
  assert.equal(cantidadForEnvase(label, 6), "6");
  assert.equal(cantidadesMenoresOf({ restosCantidades: ["5", " ", "8"] }).join(","), "5,8");
});

test("distribución exacta 5 envases con un resto", () => {
  const parts = envaseQuantities(5, "25", ["18"]);
  assert.equal(parts.length, 5);
  assert.deepEqual(parts, ["25", "25", "25", "25", "18"]);
  assert.equal(envaseDistribution(5, "25", ["18"]), "25 Pz / 25 Pz / 25 Pz / 25 Pz / 18 Pz");
  assert.equal(cantidadTotalOf({ envaseTotal: 5, cantidadPorEnvase: "25", restosCantidades: ["18"] }), "118");
  assert.equal(envaseQuantities(5, "25", ["18", "", "18"]).length, 5);
  assert.equal(cantidadesMenoresOk(["18", ""], "25", 5), true);
});
