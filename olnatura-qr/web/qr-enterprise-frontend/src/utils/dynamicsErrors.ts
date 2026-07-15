export type DynamicsErrorCode =
  | "OAUTH_CONNECTING"
  | "OAUTH_FAILED"
  | "OAUTH_TOKEN_EXPIRED"
  | "DYNAMICS_SLOW"
  | "DYNAMICS_UNREACHABLE"
  | "DYNAMICS_AUTH_REJECTED"
  | "DYNAMICS_ENTITY_ERROR"
  | "DYNAMICS_NOT_CONFIGURED"
  | "DYNAMICS_AUTH_ERROR"
  | "DYNAMICS_ODATA_ERROR"
  | "DYNAMICS_TIMEOUT"
  | "DYNAMICS_INTERNAL_ERROR";

export type DynamicsErrorView = {
  title: string;
  hint: string;
};

export const DYNAMICS_ERROR_VIEWS: Record<DynamicsErrorCode, DynamicsErrorView> = {
  OAUTH_CONNECTING: {
    title: "Conectando con Dynamics",
    hint: "Olnatura está obteniendo acceso seguro. Espera unos segundos e intenta de nuevo.",
  },
  OAUTH_FAILED: {
    title: "No se pudo autenticar con Dynamics",
    hint: "Revisa credenciales OAuth en el servidor (tenant, client id y secret).",
  },
  OAUTH_TOKEN_EXPIRED: {
    title: "Sesión con Dynamics expirada",
    hint: "Vuelve a consultar: cada búsqueda solicita un token nuevo.",
  },
  DYNAMICS_SLOW: {
    title: "Dynamics está tardando en responder",
    hint: "La consulta superó el tiempo esperado. Puede ser carga en F&O o red lenta.",
  },
  DYNAMICS_UNREACHABLE: {
    title: "Sin conexión con Dynamics",
    hint: "No se alcanzó el servidor de Finance & Operations. Verifica red y URL base.",
  },
  DYNAMICS_AUTH_REJECTED: {
    title: "Dynamics rechazó el acceso",
    hint: "El token no fue aceptado. Confirma permisos de la app en Azure y en F&O.",
  },
  DYNAMICS_ENTITY_ERROR: {
    title: "Consulta OData no completada",
    hint: "La entidad o el filtro no respondió como se esperaba en Dynamics.",
  },
  DYNAMICS_NOT_CONFIGURED: {
    title: "Integración Dynamics no configurada",
    hint: "Faltan variables OAuth en el backend o el modo no es real.",
  },
  DYNAMICS_AUTH_ERROR: {
    title: "No se pudo autenticar con Dynamics",
    hint: "Revisa tenant, client id y secret (OAuth client credentials).",
  },
  DYNAMICS_ODATA_ERROR: {
    title: "Consulta OData no completada",
    hint: "La entidad o el filtro no respondió como se esperaba en Dynamics.",
  },
  DYNAMICS_TIMEOUT: {
    title: "Dynamics está tardando en responder",
    hint: "La consulta superó el tiempo de espera configurado.",
  },
  DYNAMICS_INTERNAL_ERROR: {
    title: "Error interno al consultar Dynamics",
    hint: "Revisa logs del backend. Intenta de nuevo en unos segundos.",
  },
};

export function resolveDynamicsErrorView(body: unknown, fallbackMessage: string): DynamicsErrorView & { code?: string; elapsedMs?: number } {
  const parsed = body && typeof body === "object" ? (body as Record<string, unknown>) : {};
  const code = typeof parsed.dynamicsCode === "string"
    ? parsed.dynamicsCode
    : typeof parsed.error === "string"
      ? parsed.error
      : undefined;
  const elapsedMs = typeof parsed.elapsedMs === "number" ? parsed.elapsedMs : undefined;
  const base = code && code in DYNAMICS_ERROR_VIEWS
    ? DYNAMICS_ERROR_VIEWS[code as DynamicsErrorCode]
    : { title: "Error al consultar Dynamics", hint: fallbackMessage };
  return { ...base, code, elapsedMs };
}
