import { pushToast } from "../components/ui/toasts";

// API base URL - use "" for same-origin (when served from backend), or absolute URL for dev
const raw = import.meta.env.VITE_API_BASE_URL;
export const API_BASE =
  raw === ""
    ? ""
    : typeof raw === "string" && raw.trim().length > 0
      ? raw.trim().replace(/\/+$/, "")
      : "http://localhost:3001";

// Api error class
export class ApiError extends Error {
  status: number;
  url: string;
  body: any;

  constructor(message: string, opts: { status: number; url: string; body: any }) {
    super(message);
    this.name = "ApiError";
    this.status = opts.status;
    this.url = opts.url;
    this.body = opts.body;
  }
}

// Types
export type ApiOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: any;
  headers?: Record<string, string>;
  signal?: AbortSignal;

  // Toast global automático si hay error HTTP (default true)
  toast?: boolean;

  // Personaliza el título del toast
  toastTitle?: string;
};

// Global 401 handler
let onUnauthorized: (() => void) | null = null;

export function setOnUnauthorized(handler: () => void) {
  onUnauthorized = handler;
}

// URL joiner
function joinApiUrl(path: string) {
  // Permitir URL absoluta
  if (/^https?:\/\//i.test(path)) return path;

  const base = API_BASE.replace(/\/+$/, "");
  const p = path.startsWith("/") ? path : `/${path}`;

  // Si ya viene con /api/v1 no se duplica
  if (p === "/api/v1" || p.startsWith("/api/v1/")) return `${base}${p}`;

  return `${base}/api/v1${p}`;
}

// Helpers
function tryParseBody(text: string) {
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function pickMessage(parsed: any, fallback: string) {
  if (!parsed) return fallback;
  if (typeof parsed === "string") return parsed;
  if (typeof parsed?.message === "string") return parsed.message;
  if (typeof parsed?.error === "string") return parsed.error;
  if (typeof parsed?.detail === "string") return parsed.detail;
  return fallback;
}

function defaultToastTitle(status: number) {
  if (status === 400) return "Solicitud inválida";
  if (status === 401) return "Sesión no válida";
  if (status === 403) return "Acceso denegado";
  if (status === 404) return "No encontrado";
  if (status >= 500) return "Error del servidor";
  return "Error";
}

// API wrapper (ÚNICO acceso al backend)
export async function api<T>(path: string, opts: ApiOptions = {}): Promise<T> {
  const url = joinApiUrl(path);
  const method = opts.method ?? "GET";

  const headers: Record<string, string> = { ...(opts.headers ?? {}) };

  let body: BodyInit | undefined;
  if (opts.body !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(opts.body);
  }

  let res: Response;

  try {
    res = await fetch(url, {
      method,
      headers,
      body,
      signal: opts.signal,
      credentials: "include",
    });
  } catch (e: any) {
    if (e?.name === "AbortError") throw e;

    const ae = new ApiError("Network error", { status: 0, url, body: e ?? null });

    if (opts.toast !== false) {
      pushToast({
        intent: "error",
        title: "No se pudo conectar",
        message: "Revisa que el backend esté encendido y accesible.",
        error: ae,
      });
    }

    throw ae;
  }

  if (res.status === 204) return undefined as T;

  const text = await res.text();
  const parsed = tryParseBody(text);

  if (res.status === 401) {
    onUnauthorized?.();
  }

  if (!res.ok) {
    const msg = pickMessage(parsed, `Request failed (${res.status})`);
    const ae = new ApiError(msg, { status: res.status, url, body: parsed });

    const shouldToast = opts.toast !== false && res.status !== 401;

    if (shouldToast) {
      pushToast({
        intent: "error",
        title: opts.toastTitle ?? defaultToastTitle(res.status),
        message: msg,
        error: ae,
      });
    }

    throw ae;
  }

  return parsed as T;
}