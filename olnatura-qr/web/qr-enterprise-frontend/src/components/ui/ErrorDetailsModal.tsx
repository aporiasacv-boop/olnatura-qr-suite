import {
  Button,
  Dialog,
  DialogBody,
  DialogContent,
  DialogSurface,
  DialogTitle,
  DialogTrigger,
  makeStyles,
  shorthands,
  Text,
} from "@fluentui/react-components";
import { ApiError } from "../../api/client";

const useStyles = makeStyles({
  pre: {
    backgroundColor: "#0B1220",
    color: "#E5E7EB",
    fontSize: "12px",
    overflow: "auto",
    maxHeight: "55vh",
    ...shorthands.padding("12px"),
    ...shorthands.borderRadius("10px"),
  },
});

function safeStringify(value: any) {
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

export function ErrorDetailsModal(props: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  error: any;
}) {
  const s = useStyles();
  const { open, onOpenChange, error } = props;

  const payload =
    error instanceof ApiError
      ? { status: error.status, url: error.url, body: error.body, message: error.message }
      : { error };

  return (
    <Dialog open={open} onOpenChange={(_, d) => onOpenChange(d.open)}>
      <DialogSurface>
        <DialogBody>
          <DialogTitle>Detalles del error</DialogTitle>
          <DialogContent>
            <Text size={300}>
              Comparte esto para diagnóstico (status/url/body).
            </Text>
            <div className={s.pre} style={{ marginTop: 12 }}>
              <pre style={{ margin: 0 }}>{safeStringify(payload)}</pre>
            </div>
            <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 12 }}>
              <Button onClick={() => onOpenChange(false)}>Cerrar</Button>
            </div>
          </DialogContent>
        </DialogBody>
      </DialogSurface>
    </Dialog>
  );
}