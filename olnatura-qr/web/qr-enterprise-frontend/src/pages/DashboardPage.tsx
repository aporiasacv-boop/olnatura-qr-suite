import { useNavigate } from "react-router-dom";
import { Card, Text, makeStyles, shorthands } from "@fluentui/react-components";
import { useAuth } from "../auth/AuthContext";

const useStyles = makeStyles({
  grid: {
    display: "grid",
    gridTemplateColumns: "repeat(3, minmax(240px, 1fr))",
    ...shorthands.gap("14px"),
  },
  card: {
    ...shorthands.padding("16px"),
    cursor: "pointer",
    ...shorthands.borderRadius("12px"),
    border: "1px solid #E6E6E6",
  },
  title: { marginBottom: "4px" },
  desc: { color: "#6B6B6B" },
});

export default function DashboardPage() {
  const s = useStyles();
  const nav = useNavigate();
  const { can } = useAuth();

  return (
    <div style={{ display: "grid", rowGap: "14px" }}>
      <div>
        <Text weight="semibold" size={700}>Dashboard</Text>
        <div style={{ color: "#6B6B6B", marginTop: 4 }}>
          Acciones rápidas para operación y trazabilidad.
        </div>
      </div>

      <div className={s.grid}>
        {can("LOOKUP") && (
          <Card
            className={s.card}
            onClick={() => nav("/lookup")}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === "Enter" && nav("/lookup")}
          >
            <Text weight="semibold" className={s.title}>Buscar lote</Text>
            <div className={s.desc}>Consulta etiqueta, estado y ubicación del lote.</div>
          </Card>
        )}

        {can("ADMIN") && (
          <Card
            className={s.card}
            onClick={() => nav("/register-label")}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === "Enter" && nav("/register-label")}
          >
            <Text weight="semibold" className={s.title}>Registrar etiqueta</Text>
            <div className={s.desc}>UI placeholder. Endpoint aún no disponible.</div>
          </Card>
        )}

        <Card
          className={s.card}
          onClick={() => nav("/scan-history")}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => e.key === "Enter" && nav("/scan-history")}
        >
          <Text weight="semibold" className={s.title}>Historial de scans</Text>
          <div className={s.desc}>Revisa eventos por lote (filtro).</div>
        </Card>
      </div>
    </div>
  );
}