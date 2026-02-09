
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
    backgroundColor: brand.background,
    borderRight: `1px solid ${brand.border}`, // ✅ una sola prop
  },
  main: {
    display: "grid",
    gridTemplateRows: "56px 44px 1fr",
    backgroundColor: brand.surface,
    minWidth: 0,
  },
  content: {
    ...shorthands.padding("20px", "24px"),
    overflow: "auto",
    backgroundColor: brand.background,
  },
  surface: {
    backgroundColor: brand.surface,
    ...shorthands.borderRadius("14px"),
    ...shorthands.border("1px", "solid", brand.border), // ✅ shorthand
    ...shorthands.padding("16px"),
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