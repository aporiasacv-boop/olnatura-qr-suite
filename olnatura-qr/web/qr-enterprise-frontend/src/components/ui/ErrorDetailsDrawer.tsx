import * as React from "react";
import {
  Button,
  Drawer,
  DrawerBody,
  DrawerHeader,
  DrawerHeaderTitle,
  makeStyles,
  shorthands,
  Text,
} from "@fluentui/react-components";

const useStyles = makeStyles({
  body: { display: "grid", rowGap: "10px" },
  pre: {
    margin: 0,
    backgroundColor: "#F6F7F8",
    border: "1px solid #E6E6E6",
    ...shorthands.borderRadius("12px"),
    ...shorthands.padding("12px"),
    overflow: "auto",
    fontSize: "12px",
    lineHeight: "1.35",
  },
  row: {
    display: "grid",
    rowGap: "6px",
  },
  muted: { color: "#6B6B6B" },
});

export default function ErrorDetailsDrawer({
  open,
  onOpenChange,
  payload,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  payload: any;
}) {
  const s = useStyles();

  const json = React.useMemo(() => {
    try {
      return payload ? JSON.stringify(payload, null, 2) : "";
    } catch {
      return String(payload ?? "");
    }
  }, [payload]);

  return (
    <Drawer
      type="overlay"
      position="end"
      open={open}
      onOpenChange={(_, data) => onOpenChange(data.open)}
    >
      <DrawerHeader>
        <DrawerHeaderTitle
          action={
            <Button appearance="subtle" onClick={() => onOpenChange(false)}>
              Cerrar
            </Button>
          }
        >
          Detalle técnico
        </DrawerHeaderTitle>
      </DrawerHeader>

      <DrawerBody>
        <div className={s.body}>
          <div className={s.row}>
            <Text weight="semibold">Qué significa</Text>
            <Text className={s.muted} size={300}>
              Esto es para diagnóstico. Puedes copiar el JSON y compartirlo con backend/QA.
            </Text>
          </div>

          <pre className={s.pre}>{json}</pre>

          <Button
            appearance="primary"
            onClick={async () => {
              try {
                await navigator.clipboard.writeText(json);
              } catch {
                // silencioso: si el browser bloquea clipboard, no rompemos UX
              }
            }}
          >
            Copiar JSON
          </Button>
        </div>
      </DrawerBody>
    </Drawer>
  );
}