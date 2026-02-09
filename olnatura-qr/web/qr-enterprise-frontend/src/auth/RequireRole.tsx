import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";

type Props = {
  anyOf: string[];
  children: ReactNode;
};

export function RequireRole({ anyOf, children }: Props) {
  const { me, loading } = useAuth();
  const loc = useLocation();

  if (loading) return null;

  if (!me) {
    return <Navigate to="/login" state={{ from: loc.pathname }} replace />;
  }

  const roles = (me.roles ?? []).map((r: string) => r.toUpperCase());
  const allowed = anyOf.some((r) => roles.includes(r.toUpperCase()));

  if (!allowed) {
    // “403 Forbidden” - redirigir al home
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}