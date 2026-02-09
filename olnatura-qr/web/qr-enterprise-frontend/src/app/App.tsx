import { RouterProvider } from "react-router-dom";
import { FluentProvider, webLightTheme } from "@fluentui/react-components";
import { router } from "./router";
import { brandTheme } from "../styles/brand"; // si lo tienes
import { ToastsProvider } from "../components/ui/useToasts";
import { ToastHost } from "../components/ui/toasts";

export default function App() {
  return (
    <FluentProvider theme={{ ...webLightTheme, ...brandTheme }}>
      <ToastsProvider>
        <ToastHost />
        <RouterProvider router={router} />
      </ToastsProvider>
    </FluentProvider>
  );
}