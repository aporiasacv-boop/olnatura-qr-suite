import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbButton,
} from "@fluentui/react-components";
import { useNavigate } from "react-router-dom";
import { Text } from "@fluentui/react-components";
import { LABELS } from "../../utils/displayLabels";
import { brand } from "../../styles/brand";

function labelFor(seg: string) {
  if (!seg) return "Inicio";
  if (seg === "lookup") return LABELS.lookup;
  if (seg === "scan-history") return LABELS.scanHistory;
  if (seg === "register-label") return "Registrar etiqueta";
  if (seg === "generate-qr") return "Generar etiqueta";
  if (seg === "admin") return "Administración";
  if (seg === "approval") return "Aprobar usuarios";
  if (seg === "audit") return LABELS.auditLog;
  return seg;
}

export default function BreadcrumbsBar({ path }: { path: string }) {
  const navigate = useNavigate();
  const parts = path.split("/").filter(Boolean);

  let acc = "";
  return (
    <div
      style={{
        padding: "10px 24px",
        borderBottom: `1px solid ${brand.border}`,
        backgroundColor: brand.surface,
      }}
    >
      <Breadcrumb>
        <BreadcrumbItem>
          <BreadcrumbButton onClick={() => navigate("/")}>Inicio</BreadcrumbButton>
        </BreadcrumbItem>
        {parts.map((p) => {
          acc += `/${p}`;
          const to = acc;
          const label = labelFor(p);
          const isAdminCategory = p === "admin";
          return (
            <BreadcrumbItem key={to}>
              {isAdminCategory ? (
                <Text style={{ color: brand.muted, fontSize: 14 }}>{label}</Text>
              ) : (
                <BreadcrumbButton onClick={() => navigate(to)}>
                  {label}
                </BreadcrumbButton>
              )}
            </BreadcrumbItem>
          );
        })}
      </Breadcrumb>
    </div>
  );
}
