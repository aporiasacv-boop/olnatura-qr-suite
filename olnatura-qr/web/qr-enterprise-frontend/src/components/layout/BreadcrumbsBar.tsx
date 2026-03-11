import React from "react";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbButton,
} from "@fluentui/react-components";
import { useNavigate } from "react-router-dom";
import { Text } from "@fluentui/react-components";
import { LABELS } from "../../utils/displayLabels";

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
        padding: "10px 20px",
        borderBottom: "1px solid #E6E6E6",
        background: "#FFFFFF",
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
                <Text style={{ color: "#6B6B6B", fontSize: 14 }}>{label}</Text>
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