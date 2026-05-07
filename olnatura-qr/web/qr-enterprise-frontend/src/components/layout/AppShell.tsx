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
    backgroundColor: brand.background,
  },
  sidebar: {
<<<<<<< HEAD
    backgroundColor: brand.background,
=======
    backgroundColor: brand.surface,
>>>>>>> origin/cleanup/repo-sanitize
    borderRight: `1px solid ${brand.border}`,
  },
  main: {
    display: "grid",
    gridTemplateRows: "56px 44px 1fr",
    backgroundColor: brand.background,
    minWidth: 0,
  },
  content: {
    ...shorthands.padding("24px"),
    overflow: "auto",
    backgroundColor: brand.background,
  },
  surface: {
    backgroundColor: brand.surface,
<<<<<<< HEAD
    ...shorthands.borderRadius("14px"),
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.padding("16px"),
=======
    borderRadius: "12px",
    boxShadow: "0 1px 3px rgba(0,0,0,0.06)",
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.padding("24px"),
>>>>>>> origin/cleanup/repo-sanitize
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
          <div className={s.surface}>
            <Outlet />
          </div>
        </main>
      </section>
    </div>
  );
}
