export function labelHeaderTitle(tipoMaterial: string | null | undefined): string {
  const s = (tipoMaterial ?? "")
    .trim()
    .toUpperCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^A-Z0-9]+/g, "_");
  if (
    s === "MATERIA_PRIMA" ||
    s === "MP" ||
    s === "MPM" ||
    s === "MPS" ||
    (s.includes("MATERIA") && s.includes("PRIMA"))
  ) {
    return "MATERIA PRIMA";
  }
  return "MATERIAL DE ACONDICIONADO";
}
