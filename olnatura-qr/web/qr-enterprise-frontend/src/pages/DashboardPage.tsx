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
  },
  cardTitle: {
    fontSize: "16px",
    fontWeight: 600,
    color: brand.text,
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
          </AppCard>
        )}

        {can("REGISTER_LABEL") && (
          <AppCard
            clickable
            className={s.card}
            onClick={() => nav("/register-label")}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === "Enter" && nav("/register-label")}
          >
            <div className={s.cardTitle}>Registrar etiqueta</div>
          </AppCard>
        )}

        {can("GENERATE_LABEL") && (
          <AppCard
            clickable
            className={s.card}
            onClick={() => nav("/generate-qr")}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === "Enter" && nav("/generate-qr")}
          >
            <div className={s.cardTitle}>Generar etiqueta</div>
          </AppCard>
        )}

        {can("SCAN") && (
          <AppCard
            clickable
            className={s.card}
            onClick={() => nav("/scan-history")}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === "Enter" && nav("/scan-history")}
          >
            <div className={s.cardTitle}>Historial de escaneos</div>
          </AppCard>
        )}

        {can("AUDIT") && (
          <AppCard
            clickable
            className={s.card}
            onClick={() => nav("/admin/audit")}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === "Enter" && nav("/admin/audit")}
          >
            <div className={s.cardTitle}>Historial de auditoría</div>
          </AppCard>
        )}

        {hasRole("ADMIN") && (
          <AppCard
            clickable
            className={s.card}
            onClick={() => nav("/admin/metrics")}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === "Enter" && nav("/admin/metrics")}
          >
            <div className={s.cardTitle}>Métricas operativas</div>
          </AppCard>
        )}
      </div>
    </div>
  );
}
