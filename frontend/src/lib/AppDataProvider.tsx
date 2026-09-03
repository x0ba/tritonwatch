import { useAuth0 } from "@auth0/auth0-react";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { getMe, upsertMe } from "./api";
import type { UserProfile, WatchlistItem } from "./types";
import { useAccessToken } from "./useAccessToken";

type AppDataContextValue = {
  profile: UserProfile | null;
  profileLoading: boolean;
  profileError: string | null;
  refreshProfile: () => Promise<void>;
  setProfile: (profile: UserProfile | null) => void;
  watches: WatchlistItem[];
  addLocalWatch: (item: WatchlistItem) => void;
};

const AppDataContext = createContext<AppDataContextValue | null>(null);

export function AppDataProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading, user } = useAuth0();
  const { getToken } = useAccessToken();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [watches, setWatches] = useState<WatchlistItem[]>([]);

  const refreshProfile = useCallback(async () => {
    if (!isAuthenticated) {
      setProfile(null);
      return;
    }

    setProfileLoading(true);
    setProfileError(null);
    try {
      const token = await getToken();
      let next = await getMe(token);
      if (!next) {
        next = await upsertMe(token, {
          displayName: user?.name ?? null,
          email: user?.email ?? null,
        });
      }
      setProfile(next);
    } catch (error) {
      setProfileError(error instanceof Error ? error.message : "Failed to load profile");
    } finally {
      setProfileLoading(false);
    }
  }, [getToken, isAuthenticated, user?.email, user?.name]);

  useEffect(() => {
    if (isLoading) return;
    if (!isAuthenticated) {
      setProfile(null);
      return;
    }
    void refreshProfile();
  }, [isAuthenticated, isLoading, refreshProfile]);

  const addLocalWatch = useCallback((item: WatchlistItem) => {
    setWatches((current) => {
      if (current.some((watch) => watch.courseId === item.courseId)) {
        return current;
      }
      return [item, ...current];
    });
  }, []);

  const value = useMemo(
    () => ({
      profile,
      profileLoading,
      profileError,
      refreshProfile,
      setProfile,
      watches,
      addLocalWatch,
    }),
    [addLocalWatch, profile, profileError, profileLoading, refreshProfile, watches],
  );

  return <AppDataContext.Provider value={value}>{children}</AppDataContext.Provider>;
}

export function useAppData() {
  const ctx = useContext(AppDataContext);
  if (!ctx) {
    throw new Error("useAppData must be used within AppDataProvider");
  }
  return ctx;
}
