// src/styles/brand.ts
import type { PartialTheme } from "@fluentui/react-components";

export const brand = {
  primary: "#1E7A4A",
  hover: "#16603A",
  pressed: "#0F5132",
  soft: "#E6F4EC",
  accent: "#A3D9B1",
  background: "#F6F7F8",
  surface: "#FFFFFF",
  border: "#E5E7EB",
  borderStrong: "#D1D5DB",
  text: "#111827",
  text2: "#374151",
  muted: "#6B7280",
  successFg: "#16603A",
  successBg: "#E6F4EC",
  warningFg: "#92400E",
  warningBg: "#FEF3C7",
  dangerFg: "#991B1B",
  dangerBg: "#FEE2E2",
  infoFg: "#1E7A4A",
  infoBg: "#E0F2FE",
};

export const brandTheme: PartialTheme = {
  /* Brand */
  colorBrandForeground1: brand.primary,
  colorBrandForeground2: brand.hover,

  colorBrandBackground: brand.primary,
  colorBrandBackgroundHover: brand.hover,
  colorBrandBackgroundPressed: brand.pressed,

  /* Neutral backgrounds */
  colorNeutralBackground1: brand.surface,
  colorNeutralBackground2: "#F9FAFB",
  colorNeutralBackground3: "#F3F4F6",

  /* Text */
  colorNeutralForeground1: brand.text,
  colorNeutralForeground2: brand.text2,
  colorNeutralForeground3: brand.muted,

  /* Borders / strokes */
  colorNeutralStroke1: brand.border,
  colorNeutralStroke2: brand.borderStrong,
};