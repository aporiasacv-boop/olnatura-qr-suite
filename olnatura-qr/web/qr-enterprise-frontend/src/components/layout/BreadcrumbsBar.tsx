import React from "react";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbButton,
} from "@fluentui/react-components";
import { useNavigate } from "react-router-dom";

function labelFor(seg: string) {
  if (!seg) return "Home";
  if (seg === "lookup") return "Batch Lookup";
  if (seg === "scan-history") return "Scan History";
  if (seg === "register-label") return "Registrar etiqueta";
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
          <BreadcrumbButton onClick={() => navigate("/")}>Home</BreadcrumbButton>
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