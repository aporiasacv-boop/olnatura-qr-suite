
import React from "react";
import ReactDOM from "react-dom/client";
import { RouterProvider } from "react-router-dom";
import { FluentProvider } from "@fluentui/react-components";

import { router } from "./app/router";
import { brandTheme } from "./styles/brand";
import { ToastsProvider } from "./components/ui/toasts";
import { AuthProvider } from "./auth/AuthContext";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <FluentProvider theme={brandTheme}>
      <ToastsProvider>
        <AuthProvider>
          <RouterProvider router={router} />
        </AuthProvider>
      </ToastsProvider>
    </FluentProvider>
  </React.StrictMode>
);