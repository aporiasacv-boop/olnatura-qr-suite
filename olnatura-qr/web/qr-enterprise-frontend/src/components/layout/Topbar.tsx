import { Button, makeStyles, Text } from "@fluentui/react-components";
import { useAuth } from "../../auth/AuthContext";
import { brand } from "../../styles/brand";

const useStyles = makeStyles({
  root: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    padding: "0 24px",
    borderBottom: `1px solid ${brand.border}`,
    backgroundColor: "rgba(253, 251, 235, 0.78)",
    backdropFilter: "blur(8px)",
  },
  right: {
    display: "flex",
    alignItems: "center",
    gap: "16px",
  },
  meta: {
    color: brand.muted,
    fontSize: "12px",
  },
  title: {
    fontWeight: 600,
    fontSize: "16px",
    color: brand.text,
  },
});

export default function Topbar() {
  const s = useStyles();
  const { state, logout } = useAuth();
  const username = state.status === "authenticated" ? state.user.username : "";
  const roles = state.status === "authenticated" ? state.user.roles.join(", ") : "";

  return (
    <header className={s.root}>
      <Text className={s.title}>Sistema Olnatura</Text>
      <div className={s.right}>
        <div>
          <div><Text weight="semibold">{username}</Text></div>
          <div className={s.meta}>{roles}</div>
        </div>
        <Button appearance="secondary" onClick={() => void logout()}>
          Cerrar sesión
        </Button>
      </div>
    </header>
  );
}
