import { NavLink } from "react-router-dom";
import { makeStyles, shorthands } from "@fluentui/react-components";
import { useAuth } from "../../auth/AuthContext";
import { brand } from "../../styles/brand";
import BrandLogo from "../ui/BrandLogo";

function clsx(...parts: Array<string | false | null | undefined>) {
  return parts.filter(Boolean).join(" ");
}

const useStyles = makeStyles({
  root: {
    display: "grid",
    gridTemplateRows: "auto 1fr auto",
    height: "100%",
    ...shorthands.padding("16px", "12px"),
    backgroundColor: "transparent",
  },
  brandBlock: {
    ...shorthands.padding("8px", "10px", "16px"),
  },
  nav: {
    display: "grid",
    gap: "4px",
    alignContent: "start",
  },
  sectionLabel: {
    fontSize: "11px",
    fontWeight: 700,
    letterSpacing: "0.04em",
    textTransform: "uppercase",
    color: brand.muted,
    ...shorthands.padding("14px", "10px", "6px"),
  },
  link: {
    display: "block",
    ...shorthands.padding("10px", "12px"),
    borderRadius: "8px",
    color: brand.text2,
    textDecoration: "none",
    fontSize: "14px",
    fontWeight: 500,
    cursor: "pointer",
    transition: "background-color 0.2s ease, transform 0.2s ease, color 0.2s ease",
  },
  linkHover: {
    ":hover": {
      backgroundColor: "rgba(239, 241, 161, 0.55)",
      transform: "translateX(2px)",
    },
  },
  active: {
    backgroundColor: brand.primarySoft,
    color: brand.text,
    fontWeight: 600,
  },
  footer: {
    ...shorthands.padding("12px", "10px", "4px"),
    borderTop: `1px solid ${brand.border}`,
  },
  small: {
    fontSize: "12px",
    color: brand.muted,
  },
});

export default function Sidebar() {
  const s = useStyles();
  const { me, can, hasRole } = useAuth();
  const userLabel = me?.username ?? "—";

  const showHome = can("HOME") || hasRole("ADMIN");
  const showLookup = can("LOOKUP");
  const showScan = can("SCAN");
  const showGenerate = can("GENERATE_LABEL");
  const showRegister = can("REGISTER_LABEL");
  const showAudit = can("AUDIT");
  const showAdmin = hasRole("ADMIN");

  return (
    <div className={s.root}>
      <div className={s.brandBlock}>
        <BrandLogo size={44} title="Sistema Olnatura" subtitle="QR Suite" />
      </div>

      <nav className={s.nav}>
        {(showHome || showLookup || showScan) && (
          <div className={s.sectionLabel}>Operación</div>
        )}
        {showHome && (
          <NavLink to="/" end className={({ isActive }) => clsx(s.link, s.linkHover, isActive && s.active)}>
            Panel principal
          </NavLink>
        )}
        {showLookup && (
          <NavLink to="/lookup" className={({ isActive }) => clsx(s.link, s.linkHover, isActive && s.active)}>
            Consulta por lote
          </NavLink>
        )}
        {showScan && (
          <NavLink to="/scan-history" className={({ isActive }) => clsx(s.link, s.linkHover, isActive && s.active)}>
            Historial de escaneos
          </NavLink>
        )}

        {(showGenerate || showRegister) && <div className={s.sectionLabel}>Etiquetas</div>}
        {showGenerate && (
          <NavLink to="/generate-qr" className={({ isActive }) => clsx(s.link, s.linkHover, isActive && s.active)}>
            Generar etiqueta
          </NavLink>
        )}
        {showRegister && (
          <NavLink to="/register-label" className={({ isActive }) => clsx(s.link, s.linkHover, isActive && s.active)}>
            Registrar etiqueta
          </NavLink>
        )}

        {(showAudit || showAdmin) && (
          <>
            <div className={s.sectionLabel}>Administración</div>
            {showAdmin && (
              <>
                <NavLink to="/admin/metrics" className={({ isActive }) => clsx(s.link, s.linkHover, isActive && s.active)}>
                  Métricas operativas
                </NavLink>
                <NavLink to="/admin/approval" className={({ isActive }) => clsx(s.link, s.linkHover, isActive && s.active)}>
                  Aprobar usuarios
                </NavLink>
                <NavLink to="/admin/users" className={({ isActive }) => clsx(s.link, s.linkHover, isActive && s.active)}>
                  Usuarios
                </NavLink>
                <NavLink to="/admin/lots" className={({ isActive }) => clsx(s.link, s.linkHover, isActive && s.active)}>
                  Lotes
                </NavLink>
              </>
            )}
            {showAudit && (
              <NavLink to="/admin/audit" className={({ isActive }) => clsx(s.link, s.linkHover, isActive && s.active)}>
                Historial de auditoría
              </NavLink>
            )}
          </>
        )}
      </nav>

      <div className={s.footer}>
        <div className={s.small}>Sesión: {userLabel}</div>
      </div>
    </div>
  );
}
