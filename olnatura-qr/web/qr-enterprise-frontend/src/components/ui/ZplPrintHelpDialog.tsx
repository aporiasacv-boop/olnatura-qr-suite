import {
  Button,
  Dialog,
  DialogActions,
  DialogBody,
  DialogContent,
  DialogSurface,
  DialogTitle,
} from "@fluentui/react-components";

export const ZPL_PRINT_STEPS = [
  "Abrir Zebra Setup Utilities",
  "Seleccionar la impresora GKK420t",
  'Ir a "Open Printer Tools"',
  'Seleccionar "Action"',
  'Abrir "Send File"',
  "Cargar el archivo ZPL recién descargado.",
  "Se imprimirán las etiquetas automáticamente.",
] as const;

/** Instructivo unificado de impresión ZPL (misma leyenda en toda la app). */
export default function ZplPrintHelpDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <Dialog open={open} onOpenChange={(_, data) => onOpenChange(data.open)}>
      <DialogSurface>
        <DialogBody>
          <DialogTitle>Cómo imprimir archivos ZPL</DialogTitle>
          <DialogContent>
            <ol style={{ margin: "8px 0 0", paddingLeft: 22, lineHeight: 1.55 }}>
              {ZPL_PRINT_STEPS.map((step) => (
                <li key={step} style={{ marginBottom: 6 }}>
                  {step}
                </li>
              ))}
            </ol>
          </DialogContent>
          <DialogActions>
            <Button appearance="primary" onClick={() => onOpenChange(false)}>
              Cerrar
            </Button>
          </DialogActions>
        </DialogBody>
      </DialogSurface>
    </Dialog>
  );
}
