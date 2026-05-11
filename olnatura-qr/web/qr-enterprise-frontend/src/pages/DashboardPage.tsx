import { useNavigate } from "react-router-dom";
import { makeStyles, shorthands } from "@fluentui/react-components";
import { useAuth } from "../auth/AuthContext";
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";

const useStyles = makeStyles({
  page: {
    display: "grid",
    gap: "24px",
  },
  title: {
    fontSize: "20px",
    fontWeight: 600,
    color: brand.text,
  },
  grid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))",
    ...shorthands.gap("16px"),
  },
  card: {
    ...shorthands.padding("20px"),
    transition: "box-shadow 0.2s ease",
    ":hover": {
      boxShadow: "0 2px 8px rgba(0,0,0,0.08)",
    },
  },
  cardTitle: {
    fontSize: "16px",
    fontWeight: 600,
    color: brand.text,
    marginBottom: "8px",
  },
  cardDesc: {
    fontSize: "14px",
    color: brand.muted,
    lineHeight: 1.5,
  },
});

export default function DashboardPage() {
  const s = useStyles();
  const nav = useNavigate();
  const { can, hasRole } = useAuth();

  return (
    <div className={s.page}>
      <h1 className={s.title}>Panel principal</h1>

      <div className={s.grid}>
        {can("LOOKUP") && (
          <AppCard
            clickable
            className={s.card}
            onClick={() => nav("/lookup")}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === "Enter" && nav("/lookup")}
          >
            <div className={s.cardTitle}>Buscar lote</div>
            <div className={s.cardDesc}>Consulta etiqueta, estado y ubicación del lote.</div>
          </AppCard>
        )}

        {(hasRole("ADMIN") || hasRole("ALMACEN")) && (
          <AppCard
            clickable
            className={s.card}
            onClick={() => nav("/register-label")}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === "Enter" && nav("/register-label")}
          >
            <div className={s.cardTitle}>Registrar etiqueta</div>
            <div className={s.cardDesc}>Registrar nueva etiqueta en el sistema.</div>
          </AppCard>
        )}

        <AppCard
          clickable
          className={s.card}
          onClick={() => nav("/scan-history")}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => e.key === "Enter" && nav("/scan-history")}
        >
          <div className={s.cardTitle}>Historial de escaneos</div>
          <div className={s.cardDesc}>Revisa eventos por lote.</div>
        </AppCard>
      </div>
    </div>
  );
}
