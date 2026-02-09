import * as React from "react";
import {
  Toaster,
  Toast,
  ToastTitle,
  ToastBody,
  useToastController,
  Link,
  makeStyles,
} from "@fluentui/react-components";
import ErrorDetailsDrawer from "./ErrorDetailsDrawer";

export type ToastIntent = "success" | "error" | "warning" | "info";

export type ToastItem = {
  intent: ToastIntent;
  title: string;
  message?: string;
  error?: unknown;

  /** evita spam (opcional) */
  dedupeKey?: string;
};

type ToastsContextValue = {
  push: (t: ToastItem) => void;
};

const ToastsContext = React.createContext<ToastsContextValue | null>(null);

const TOASTER_ID = "global-toaster";

/** Bridge global: permite disparar toast desde client.ts sin hooks */
let globalPush: ((t: ToastItem) => void) | null = null;

export function pushToast(t: ToastItem) {
  globalPush?.(t);
}

const useStyles = makeStyles({
  wrap: {
    position: "fixed",
    top: "12px",
    left: "50%",
    transform: "translateX(-50%)",
    zIndex: 10000,
    width: "min(720px, calc(100vw - 24px))",
    pointerEvents: "none",
  },
  toastInteractive: {
    pointerEvents: "auto",
  },
});

function isApiErrorLike(x: any) {
  return (
    x &&
    typeof x === "object" &&
    (x.name === "ApiError" || typeof x.status === "number") &&
    typeof x.url === "string" &&
    "body" in x
  );
}

function normalizeError(err: unknown) {
  const ts = new Date().toISOString();
  if (!err) return { kind: "unknown", message: "Unknown error", ts, raw: err };

  if (isApiErrorLike(err)) {
    return {
      kind: "api",
      message: String((err as any).message ?? "API error"),
      status: (err as any).status,
      url: (err as any).url,
      body: (err as any).body,
      ts,
      raw: err,
    };
  }

  if (err instanceof Error) {
    return { kind: "error", message: err.message, stack: err.stack, ts, raw: err };
  }

  return { kind: "unknown", message: String(err), ts, raw: err };
}

export function ToastsProvider({ children }: { children: React.ReactNode }) {
  const s = useStyles();
  const { dispatchToast } = useToastController(TOASTER_ID);

  const [drawerOpen, setDrawerOpen] = React.useState(false);
  const [drawerPayload, setDrawerPayload] = React.useState<any>(null);

  const lastDedupeRef = React.useRef<{ key: string; at: number } | null>(null);

  const openDetails = React.useCallback((err: unknown) => {
    setDrawerPayload(normalizeError(err));
    setDrawerOpen(true);
  }, []);

  const push = React.useCallback(
    (t: ToastItem) => {
      if (t.dedupeKey) {
        const now = Date.now();
        const prev = lastDedupeRef.current;
        if (prev && prev.key === t.dedupeKey && now - prev.at < 800) return;
        lastDedupeRef.current = { key: t.dedupeKey, at: now };
      }

      const hasDetails = Boolean(t.error);

      dispatchToast(
        <Toast className={s.toastInteractive}>
          <ToastTitle>{t.title}</ToastTitle>

          {t.message ? (
            <ToastBody>
              {t.message}{" "}
              {hasDetails ? (
                <Link
                  onClick={(e) => {
                    e.preventDefault();
                    openDetails(t.error);
                  }}
                >
                  Ver más
                </Link>
              ) : null}
            </ToastBody>
          ) : hasDetails ? (
            <ToastBody>
              <Link
                onClick={(e) => {
                  e.preventDefault();
                  openDetails(t.error);
                }}
              >
                Ver más
              </Link>
            </ToastBody>
          ) : null}
        </Toast>,
        { intent: t.intent, timeout: t.intent === "error" ? 7000 : 4000 }
      );
    },
    [dispatchToast, openDetails, s.toastInteractive]
  );

  React.useEffect(() => {
    globalPush = push;
    return () => {
      if (globalPush === push) globalPush = null;
    };
  }, [push]);

  const value = React.useMemo<ToastsContextValue>(() => ({ push }), [push]);

  return (
    <ToastsContext.Provider value={value}>
      <div className={s.wrap}>
        <Toaster toasterId={TOASTER_ID} />
      </div>

      <ErrorDetailsDrawer
        open={drawerOpen}
        onOpenChange={setDrawerOpen}
        payload={drawerPayload}
      />

      {children}
    </ToastsContext.Provider>
  );
}

export function useToasts() {
  const ctx = React.useContext(ToastsContext);
  if (!ctx) throw new Error("useToasts must be used inside ToastsProvider");
  return ctx;
}