import { createBrowserRouter } from "react-router-dom";

import AppShell from "../components/layout/AppShell";

import LoginPage from "../pages/LoginPage";
import DashboardPage from "../pages/DashboardPage";
import NotFoundPage from "../pages/NotFoundPage";

import RequestAccessPage from "../pages/RequestAccessPage";
import AdminApprovalPage from "../pages/AdminApprovalPage";

import BatchLookupPage from "../pages/BatchLookupPage";
import ScanHistoryPage from "../pages/ScanHistoryPage";
import RegisterLabelPage from "../pages/RegisterLabelPage";
import GenerateQrPage from "../pages/GenerateQrPage"; // 👈 NUEVO

import { RequireAuth, RequireAdmin } from "../auth/guards";
import { RequireRole } from "../auth/RequireRole"; // 👈 NUEVO

export const router = createBrowserRouter([
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    path: "/request-access",
    element: <RequestAccessPage />,
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

      { path: "lookup", element: <BatchLookupPage /> },
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
          <RequireAdmin>
            <RegisterLabelPage />
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

      { path: "*", element: <NotFoundPage /> },
    ],
  },
]);