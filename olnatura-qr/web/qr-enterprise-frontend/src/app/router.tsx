import { createBrowserRouter, Navigate } from "react-router-dom";

import AppShell from "../components/layout/AppShell";

import LoginPage from "../pages/LoginPage";
import DashboardPage from "../pages/DashboardPage";
import NotFoundPage from "../pages/NotFoundPage";
import RegisterRequestPage from "../pages/RegisterRequestPage";

import AdminApprovalPage from "../pages/AdminApprovalPage";
import AdminAuditPage from "../pages/AdminAuditPage";
import AdminMetricsPage from "../pages/AdminMetricsPage";
import AdminUsersPage from "../pages/AdminUsersPage";
import AdminLotsPage from "../pages/AdminLotsPage";
import AdminDbPage from "../pages/AdminDbPage";
import AdminProblemReportsPage from "../pages/AdminProblemReportsPage";

import ScanHistoryPage from "../pages/ScanHistoryPage";
import RegisterLabelPage from "../pages/RegisterLabelPage";
import GenerateQrPage from "../pages/GenerateQrPage";

import OperationalStatusValidationTempPage from "../pages/temp/OperationalStatusValidationTempPage";

import { RequireAuth, RequireAdmin } from "../auth/guards";
import { RequireRole } from "../auth/RequireRole";

export const router = createBrowserRouter([
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    path: "/register-request",
    element: <RegisterRequestPage />,
  },

  {
    path: "/",
    element: (
      <RequireAuth>
        <AppShell />
      </RequireAuth>
    ),
    errorElement: (
      <RequireAuth>
        <AppShell />
      </RequireAuth>
    ),

    children: [
      { index: true, element: <DashboardPage /> },

      { path: "lookup", element: <Navigate to="/consulta-lote" replace /> },

      { path: "scan-history", element: <ScanHistoryPage /> },

      {
        path: "generate-qr",
        element: (
          <RequireRole anyOf={["ADMIN", "ALMACEN"]}>
            <GenerateQrPage />
          </RequireRole>
        ),
      },

      {
        path: "register-label",
        element: (
          <RequireRole anyOf={["ADMIN", "ALMACEN"]}>
            <RegisterLabelPage />
          </RequireRole>
        ),
      },

      {
        path: "consulta-lote",
        element: (
          <RequireRole anyOf={["ADMIN", "ALMACEN", "PRODUCCION", "CALIDAD", "INSPECCION", "VALIDACION"]}>
            <OperationalStatusValidationTempPage />
          </RequireRole>
        ),
      },
      {
        path: "temp/operational-status-validation",
        element: <Navigate to="/consulta-lote" replace />,
      },

      {
        path: "admin/metrics",
        element: (
          <RequireAdmin>
            <AdminMetricsPage />
          </RequireAdmin>
        ),
      },
      {
        path: "admin/approval",
        element: (
          <RequireAdmin>
            <AdminApprovalPage />
          </RequireAdmin>
        ),
      },
      {
        path: "admin/users",
        element: (
          <RequireAdmin>
            <AdminUsersPage />
          </RequireAdmin>
        ),
      },
      {
        path: "admin/lots",
        element: (
          <RequireAdmin>
            <AdminLotsPage />
          </RequireAdmin>
        ),
      },
      {
        path: "admin/db",
        element: (
          <RequireAdmin>
            <AdminDbPage />
          </RequireAdmin>
        ),
      },
      {
        path: "admin/problem-reports",
        element: (
          <RequireAdmin>
            <AdminProblemReportsPage />
          </RequireAdmin>
        ),
      },
      {
        path: "admin/audit",
        element: (
          <RequireRole anyOf={["ADMIN", "ALMACEN", "PRODUCCION", "CALIDAD", "INSPECCION", "VALIDACION"]}>
            <AdminAuditPage />
          </RequireRole>
        ),
      },

      { path: "*", element: <NotFoundPage /> },
    ],
  },
]);
