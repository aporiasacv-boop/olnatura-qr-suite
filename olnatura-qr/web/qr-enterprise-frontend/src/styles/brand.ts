import type { PartialTheme } from "@fluentui/react-components";

export const brand = {
  primary: "#9CAF47",
  hover: "#8A9E3C",
  pressed: "#7A8E35",
  primarySoft: "#EFF1A1",
  accent: "#B8C95A",
  background: "#FDFBEB",
  surface: "rgba(255, 254, 248, 0.88)",
  surfaceSolid: "#FFFEF8",
  border: "#E5E7EB",
  borderStrong: "#D1D5DB",
  text: "#1F2937",
  text2: "#374151",
  muted: "#6B7280",
  successFg: "#16603A",
  successBg: "#E6F4EC",
  warningFg: "#92400E",
  warningBg: "#FEF3C7",
  dangerFg: "#991B1B",
  dangerBg: "#FEE2E2",
  infoFg: "#4A7C59",
  infoBg: "#E8F5EC",
};

export const brandTheme: PartialTheme = {
  colorBrandForeground1: brand.primary,
  colorBrandForeground2: brand.hover,

  colorBrandBackground: brand.primary,
  colorBrandBackgroundHover: brand.hover,
  colorBrandBackgroundPressed: brand.pressed,

  colorNeutralBackground1: "#FFFEF8",
  colorNeutralBackground2: "#F8F6E4",
  colorNeutralBackground3: "#F1F2EE",

  colorNeutralForeground1: brand.text,
  colorNeutralForeground2: brand.text2,
  colorNeutralForeground3: brand.muted,

  colorNeutralStroke1: brand.border,
  colorNeutralStroke2: brand.borderStrong,

  borderRadiusMedium: "8px",
  borderRadiusLarge: "10px",
};
