import { Outlet, useLocation } from "react-router-dom";
import { makeStyles, shorthands } from "@fluentui/react-components";
import Sidebar from "./Sidebar";
import Topbar from "./Topbar";
import BreadcrumbsBar from "./BreadcrumbsBar";
import { brand } from "../../styles/brand";

const useStyles = makeStyles({
  root: {
    display: "grid",
    gridTemplateColumns: "260px 1fr",
    height: "100vh",
    backgroundColor: "transparent",
  },
  sidebar: {
    backgroundColor: "rgba(253, 251, 235, 0.72)",
    backdropFilter: "blur(8px)",
    borderRight: `1px solid ${brand.border}`,
  },
  main: {
    display: "grid",
    gridTemplateRows: "56px 44px 1fr",
    backgroundColor: "transparent",
    minWidth: 0,
  },
  content: {
    ...shorthands.padding("24px"),
    overflow: "auto",
    backgroundColor: "transparent",
  },
  surface: {
    backgroundColor: brand.surface,
    backdropFilter: "blur(8px)",
    borderRadius: "12px",
    boxShadow: "0 4px 18px rgba(74, 92, 40, 0.08)",
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.padding("24px"),
    minHeight: "100%",
  },
});

export default function AppShell() {
  const s = useStyles();
  const loc = useLocation();

  return (
    <div className={s.root}>
      <aside className={s.sidebar}>
        <Sidebar />
      </aside>

      <section className={s.main}>
        <Topbar />
        <BreadcrumbsBar path={loc.pathname} />
        <main className={s.content}>
          <div className={`${s.surface} app-card`}>
            <Outlet />
          </div>
        </main>
      </section>
    </div>
  );
}
