
import { NavLink } from "react-router-dom";
import { makeStyles, shorthands, Text } from "@fluentui/react-components";
import { useAuth } from "../../auth/AuthContext";
import clsx from "clsx";
import { brand } from "../../styles/brand";

const useStyles = makeStyles({
  root: {
    height: "100%",
    display: "flex",
    flexDirection: "column",
    backgroundColor: brand.background,
  },
  header: {
    ...shorthands.padding("18px", "16px"),
    borderBottom: `1px solid ${brand.border}`, // ✅ una sola prop
  },
  logoRow: { display: "flex", gap: "10px", alignItems: "center" },
  logo: { width: "28px", height: "28px", objectFit: "contain" },
  title: { fontWeight: 600, color: brand.text },
  subtitle: { color: brand.muted, fontSize: "12px" },

  nav: {
    display: "flex",
    flexDirection: "column",
    ...shorthands.padding("12px", "8px"),
    rowGap: "2px",
  },

  link: {
    display: "flex",
    alignItems: "center",
    gap: "10px",
    ...shorthands.padding("10px", "12px"),
    ...shorthands.borderRadius("10px"),
    color: brand.text,
    textDecorationLine: "none",
    ...shorthands.border("1px", "solid", "transparent"), // ✅ shorthand
  },

  linkHover: {
    ":hover": {
      backgroundColor: brand.surface,
      border: `1px solid ${brand.border}`, // ✅ una sola prop
    },
  },

  active: {
    backgroundColor: brand.soft,
    border: `1px solid ${brand.accent}`, // ✅ una sola prop
  },

  footer: {
    marginTop: "auto",
    ...shorthands.padding("14px", "16px"),
    borderTop: `1px solid ${brand.border}`, // ✅ una sola prop
  },
  small: { color: brand.muted, fontSize: "12px" },
});

export default function Sidebar() {
  const s = useStyles();
  const { state, can } = useAuth();

  const userLabel = state.status === "authenticated" ? `${state.user.username}` : "—";
  return (
    <div className={s.root}>
      <div className={s.header}>
        <div className={s.logoRow}>
          <img className={s.logo} src="/logo-olnatura.png" alt="Logo" />
          <div>
            <Text className={s.title}>Inventory System</Text>
            <div className={s.subtitle}>QR Traceability</div>
          </div>
        </div>
      </div>

      <nav className={s.nav}>
        <NavLink to="/" className={({ isActive }) => clsx(s.link, s.linkHover, isActive && s.active)}>
          Dashboard
        </NavLink>

        {can("LOOKUP") && (
          <NavLink to="/lookup" className={({ isActive }) => clsx(s.link, s.linkHover, isActive && s.active)}>
            Batch Lookup
          </NavLink>
        )}

        <NavLink to="/scan-history" className={({ isActive }) => clsx(s.link, s.linkHover, isActive && s.active)}>
          Scan History
        </NavLink>

        {can("ADMIN") && (
          <NavLink to="/register-label" className={({ isActive }) => clsx(s.link, s.linkHover, isActive && s.active)}>
            Registrar etiqueta
          </NavLink>
        )}
      </nav>

      <div className={s.footer}>
        <div className={s.small}>Sesión: {userLabel}</div>
      </div>
    </div>
  );
}