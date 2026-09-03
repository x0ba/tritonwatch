import { useAuth0 } from "@auth0/auth0-react";
import { useCallback } from "react";
import { AUTH0_SCOPES } from "./format";

export function useAccessToken() {
  const { getAccessTokenSilently, isAuthenticated } = useAuth0();

  const getToken = useCallback(
    async (scope?: string) => {
      return getAccessTokenSilently({
        authorizationParams: {
          audience: import.meta.env.VITE_AUTH0_AUDIENCE,
          scope: scope ?? AUTH0_SCOPES,
        },
      });
    },
    [getAccessTokenSilently],
  );

  return { getToken, isAuthenticated };
}
