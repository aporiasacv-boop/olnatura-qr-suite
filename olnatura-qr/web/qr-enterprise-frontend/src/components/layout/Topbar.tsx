import React from "react";
import { Button, makeStyles, Text } from "@fluentui/react-components";
import { useAuth } from "../../auth/AuthContext";
import { api } from "../../api/client";

const useStyles = makeStyles({
  root: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    padding: "0 20px",
    borderBottom: "1px solid #E6E6E6",
    backgroundColor: "#FFFFFF",
  },
  right: { display: "flex", alignItems: "center", gap: "12px" },
  meta: { color: "#6B6B6B" },
});

export default function Topbar() {
  const s = useStyles();
  const { state, logout } = useAuth();

  const username = state.status === "authenticated" ? state.user.username : "";
  const roles = state.status === "authenticated" ? state.user.roles.join(", ") : "";

  return (
    <header className={s.root}>
      <Text weight="semibold">Panel interno Olnatura</Text>
      <div className={s.right}>
        <div>
          <div><Text weight="semibold">{username}</Text></div>
          <div className={s.meta}><Text size={200}>{roles}</Text></div>
        </div>
        <Button appearance="secondary" onClick={() => void logout()}>
          Cerrar sesión
        </Button>
      </div>
    </header>
  );
}