import React from "react";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbButton,
} from "@fluentui/react-components";
import { useNavigate } from "react-router-dom";

function labelFor(seg: string) {
  if (!seg) return "Inicio";
  if (seg === "lookup") return "Consulta por lote";
  if (seg === "scan-history") return "Historial de escaneos";
  if (seg === "register-label") return "Registrar etiqueta";
  if (seg === "generate-qr") return "Etiquetas";
  if (seg === "admin") return "Administración";
  if (seg === "approval") return "Aprobar usuarios";
  if (seg === "audit") return "Historial";
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
          return (
            <BreadcrumbItem key={to}>
              <BreadcrumbButton onClick={() => navigate(to)}>
                {labelFor(p)}
              </BreadcrumbButton>
            </BreadcrumbItem>
          );
        })}
      </Breadcrumb>
    </div>
  );
}