import * as React from "react";
import { api, setOnUnauthorized } from "../api/client";
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

  const setAnonymous = React.useCallback(() => {
    setState({ status: "anonymous", user: null });
  }, []);

  React.useEffect(() => {
    setOnUnauthorized(() => {
      setAnonymous();
    });
  }, [setAnonymous]);

  const refreshMe = React.useCallback(async () => {
    try {
      const me = await api<Me>("/auth/me");
      setState({ status: "authenticated", user: me });
    } catch (err) {
      setAnonymous();
    }
  }, [setAnonymous]);

  React.useEffect(() => {
    refreshMe();
  }, [refreshMe]);

  const login = React.useCallback(
    async (payload: LoginRequest) => {
      await api<void>("/auth/login", { method: "POST", body: payload });
      await refreshMe();
    },
    [refreshMe]
  );

  const logout = React.useCallback(async () => {
    try {
      await api<void>("/auth/logout", { method: "POST" });
    } catch {
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