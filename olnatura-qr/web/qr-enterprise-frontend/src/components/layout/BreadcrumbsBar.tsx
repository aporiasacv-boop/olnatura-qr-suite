import { Fragment } from "react";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbButton,
  BreadcrumbDivider,
  makeStyles,
  Text,
  tokens,
} from "@fluentui/react-components";
import { useNavigate } from "react-router-dom";
import { LABELS } from "../../utils/displayLabels";
import { brand } from "../../styles/brand";

function labelFor(seg: string) {
  if (!seg) return "Inicio";
  if (seg === "lookup") return LABELS.lookupNav ?? LABELS.lookup;
  if (seg === "scan-history") return LABELS.scanHistory;
  if (seg === "register-label") return "Registrar etiqueta";
  if (seg === "generate-qr") return "Generar etiqueta";
  if (seg === "admin") return "Administración";
  if (seg === "approval") return "Aprobar usuarios";
  if (seg === "audit") return LABELS.auditLog;
  if (seg === "metrics") return LABELS.metrics;
  if (seg === "users") return "Usuarios";
  if (seg === "lots") return "Lotes";
  return seg;
}

type Crumb = {
  to: string;
  label: string;
  /** Category segment without its own route (e.g. /admin). */
  nonNavigable?: boolean;
};

const useStyles = makeStyles({
  bar: {
    padding: "10px 24px",
    borderBottom: `1px solid ${brand.border}`,
    backgroundColor: "rgba(228, 232, 212, 0.92)",
    backdropFilter: "blur(8px)",
  },
  crumb: {
    color: brand.muted,
    fontSize: tokens.fontSizeBase200,
    fontWeight: tokens.fontWeightRegular,
  },
  current: {
    color: brand.text2,
    fontSize: tokens.fontSizeBase200,
    fontWeight: tokens.fontWeightSemibold,
  },
  divider: {
    color: brand.borderStrong,
    margin: "0 2px",
  },
});

export default function BreadcrumbsBar({ path }: { path: string }) {
  const s = useStyles();
  const navigate = useNavigate();
  const parts = path.split("/").filter(Boolean);

  const crumbs: Crumb[] = [{ to: "/", label: "Inicio" }];
  let acc = "";
  for (const p of parts) {
    acc += `/${p}`;
    crumbs.push({
      to: acc,
      label: labelFor(p),
      nonNavigable: p === "admin",
    });
  }

  return (
    <div className={s.bar}>
      <Breadcrumb aria-label="Ruta de navegación">
        {crumbs.map((crumb, i) => {
          const isLast = i === crumbs.length - 1;
          return (
            <Fragment key={crumb.to}>
              {i > 0 && <BreadcrumbDivider className={s.divider} />}
              <BreadcrumbItem>
                {isLast ? (
                  <Text className={s.current} aria-current="page">
                    {crumb.label}
                  </Text>
                ) : crumb.nonNavigable ? (
                  <Text className={s.crumb}>{crumb.label}</Text>
                ) : (
                  <BreadcrumbButton
                    className={s.crumb}
                    onClick={() => navigate(crumb.to)}
                  >
                    {crumb.label}
                  </BreadcrumbButton>
                )}
              </BreadcrumbItem>
            </Fragment>
          );
        })}
      </Breadcrumb>
    </div>
  );
}
