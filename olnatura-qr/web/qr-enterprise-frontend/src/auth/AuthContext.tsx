import * as React from "react";
import { api, setOnUnauthorized, ApiError } from "../api/client";
import type { LoginRequest, Me, Role } from "../api/types";

type AuthState =
  | { status: "loading"; user: null }
  | { status: "anonymous"; user: null }
  | { status: "authenticated"; user: Me };

type AuthContextValue = {
  state: AuthState;

  me: Me | null;
  loading: boolean;

  login: (payload: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  refreshMe: () => Promise<void>;

  hasRole: (role: Role) => boolean;
  can: (perm: "LOOKUP" | "SCAN" | "ADMIN") => boolean;
};

const AuthContext = React.createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = React.useState<AuthState>({
    status: "loading",
    user: null,
  });

  // ---- helpers de estado ----
  const setAnonymous = React.useCallback(() => {
    setState({ status: "anonymous", user: null });
  }, []);

  // ---- 401 global ----
  React.useEffect(() => {
    // Cuando el client detecte 401, limpia sesión local.
    setOnUnauthorized(() => {
      setAnonymous();
    });
  }, [setAnonymous]);

  // ---- fetch /me ----
  const refreshMe = React.useCallback(async () => {
    try {
      const me = await api<Me>("/auth/me");
      setState({ status: "authenticated", user: me });
    } catch (err) {
      // Si /me falla (401 o backend caído), queda como anónimo
      setAnonymous();
    }
  }, [setAnonymous]);

  React.useEffect(() => {
    refreshMe();
  }, [refreshMe]);

  // ---- login ----
  const login = React.useCallback(
    async (payload: LoginRequest) => {
      await api<void>("/auth/login", { method: "POST", body: payload });
      await refreshMe();
    },
    [refreshMe]
  );

  // ---- logout ----
  const logout = React.useCallback(async () => {
    try {
      await api<void>("/auth/logout", { method: "POST" });
    } catch (err) {
      // si falla igual limpiamos localmente
      const _ae = err as ApiError;
      // (puedes loggear si quieres)
    } finally {
      setAnonymous();
    }
  }, [setAnonymous]);

  const me = state.status === "authenticated" ? state.user : null;
  const loading = state.status === "loading";

  const hasRole = React.useCallback(
    (role: Role) => {
      return !!me?.roles?.includes(role);
    },
    [me]
  );

  const can = React.useCallback(
    (perm: "LOOKUP" | "SCAN" | "ADMIN") => {
      if (!me) return false;
      if (perm === "ADMIN") return me.roles.includes("ADMIN");
      return true;
    },
    [me]
  );

  const value = React.useMemo<AuthContextValue>(
    () => ({
      state,
      me,
      loading,
      login,
      logout,
      refreshMe,
      hasRole,
      can,
    }),
    [state, me, loading, login, logout, refreshMe, hasRole, can]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = React.useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}