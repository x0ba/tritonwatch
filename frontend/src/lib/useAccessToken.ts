import { useAuth } from "@clerk/react";
import { useCallback } from "react";

export function useAccessToken() {
  const { getToken: getClerkToken, isSignedIn } = useAuth();

  const getToken = useCallback(async () => {
    const token = await getClerkToken();
    if (!token) {
      throw new Error("No active Clerk session");
    }
    return token;
  }, [getClerkToken]);

  return { getToken, isAuthenticated: Boolean(isSignedIn) };
}
