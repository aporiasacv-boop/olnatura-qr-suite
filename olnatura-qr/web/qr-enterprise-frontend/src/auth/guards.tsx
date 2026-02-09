// src/auth/guards.tsx
import type { JSX, ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";

export function RequireRole({
  anyOf,
  children,
}: {
  anyOf: string[];
  children: JSX.Element;
}) {
  const { me, loading } = useAuth();
  const loc = useLocation();

  if (loading) return null;

  if (!me) {
    return <Navigate to="/login" state={{ from: loc.pathname }} replace />;
  }

  const roles = me.roles || [];
  const allowed = anyOf.some((r) => roles.includes(r));

  if (!allowed) {
    return <Navigate to="/" replace />;
  }

  return children;
}
export function RequireAuth({ children }: { children: ReactNode }) {
  const { me, loading } = useAuth();
  const loc = useLocation();

  if (loading) return null; // o tu LoadingState
  if (!me) return <Navigate to="/login" replace state={{ from: loc.pathname }} />;

  return <>{children}</>;
}

export function RequireAdmin({ children }: { children: ReactNode }) {
  const { me, loading } = useAuth();
  const loc = useLocation();

  if (loading) return null;
  if (!me) return <Navigate to="/login" replace state={{ from: loc.pathname }} />;

  const isAdmin = Array.isArray(me.roles) && me.roles.includes("ADMIN");
  if (!isAdmin) return <Navigate to="/" replace />;

  return <>{children}</>;
}