import type { PartialTheme } from "@fluentui/react-components";

/**
 * Paleta operacional: crema + verde oliva pastel un poco más matizado
 * (menos blanco puro, sin saturación agresiva).
 */
export const brand = {
  primary: "#8FA33F",
  hover: "#7E9336",
  pressed: "#6E822F",
  primarySoft: "#E2E6A8",
  accent: "#A8BC4E",
  background: "#EDE6D0",
  surface: "rgba(240, 236, 220, 0.96)",
  surfaceSolid: "#F0ECDC",
  surfaceMuted: "#E4E8D4",
  border: "#D2CBB4",
  borderStrong: "#BDB69F",
  text: "#1A2330",
  text2: "#2F3A47",
  muted: "#5F6B78",
  successFg: "#145C36",
  successBg: "#DCEFE4",
  warningFg: "#8A3B0A",
  warningBg: "#F0DFB8",
  dangerFg: "#8F1D1D",
  dangerBg: "#F8DADA",
  infoFg: "#3F6B4C",
  infoBg: "#DCE6CC",
};

export const brandTheme: PartialTheme = {
  colorBrandForeground1: brand.primary,
  colorBrandForeground2: brand.hover,

  colorBrandBackground: brand.primary,
  colorBrandBackgroundHover: brand.hover,
  colorBrandBackgroundPressed: brand.pressed,

  colorNeutralBackground1: brand.surfaceSolid,
  colorNeutralBackground2: "#E8E2CC",
  colorNeutralBackground3: brand.surfaceMuted,

  colorNeutralForeground1: brand.text,
  colorNeutralForeground2: brand.text2,
  colorNeutralForeground3: brand.muted,

  colorNeutralStroke1: brand.border,
  colorNeutralStroke2: brand.borderStrong,

  borderRadiusMedium: "8px",
  borderRadiusLarge: "10px",
};
