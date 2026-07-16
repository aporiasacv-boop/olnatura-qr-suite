import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";

export function RequireAuth({ children }: { children: ReactNode }) {
  const { me, loading } = useAuth();
  const loc = useLocation();

  if (loading) return null; 
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