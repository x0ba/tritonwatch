import { Auth0Provider } from "@auth0/auth0-react";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { App } from "./App";
import { AppDataProvider } from "./lib/AppDataProvider";
import { AUTH0_SCOPES } from "./lib/format";
import "./index.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <Auth0Provider
      domain={import.meta.env.VITE_AUTH0_DOMAIN || "placeholder.auth0.com"}
      clientId={import.meta.env.VITE_AUTH0_CLIENT_ID || "placeholder"}
      authorizationParams={{
        redirect_uri: window.location.origin,
        audience: import.meta.env.VITE_AUTH0_AUDIENCE,
        scope: AUTH0_SCOPES,
      }}
      cacheLocation="memory"
    >
      <BrowserRouter>
        <AppDataProvider>
          <App />
        </AppDataProvider>
      </BrowserRouter>
    </Auth0Provider>
  </StrictMode>,
);
