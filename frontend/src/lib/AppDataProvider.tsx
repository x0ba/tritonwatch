import { useAuth, useUser } from "@clerk/react";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { getMe, listCatalogTerms, loadWatchlistItems, upsertMe } from "./api";
import type { UserProfile, WatchlistItem } from "./types";
import { useAccessToken } from "./useAccessToken";

export const WATCHLIST_REFRESH_INTERVAL_MS = 2 * 60 * 1000;

type AppDataContextValue = {
  profile: UserProfile | null;
  profileLoading: boolean;
  profileError: string | null;
  refreshProfile: () => Promise<void>;
  setProfile: (profile: UserProfile | null) => void;
  watches: WatchlistItem[];
  watchesLoading: boolean;
  watchesError: string | null;
  watchesLastRefreshedAt: string | null;
  refreshWatches: () => Promise<void>;
  addLocalWatch: (item: WatchlistItem) => void;
};

const AppDataContext = createContext<AppDataContextValue | null>(null);

export function AppDataProvider({ children }: { children: ReactNode }) {
  const { isLoaded, isSignedIn } = useAuth();
  const { user } = useUser();
  const { getToken } = useAccessToken();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [watches, setWatches] = useState<WatchlistItem[]>([]);
  const [watchesLoading, setWatchesLoading] = useState(false);
  const [watchesError, setWatchesError] = useState<string | null>(null);
  const [watchesLastRefreshedAt, setWatchesLastRefreshedAt] = useState<string | null>(null);
  const watchRefreshGeneration = useRef(0);

  const refreshProfile = useCallback(async () => {
    if (!isSignedIn) {
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
          displayName: user?.fullName ?? null,
          email: user?.primaryEmailAddress?.emailAddress ?? null,
        });
      }
      setProfile(next);
    } catch (error) {
      setProfileError(error instanceof Error ? error.message : "Failed to load profile");
    } finally {
      setProfileLoading(false);
    }
  }, [getToken, isSignedIn, user?.fullName, user?.primaryEmailAddress?.emailAddress]);

  const refreshWatches = useCallback(async () => {
    if (!isSignedIn) {
      watchRefreshGeneration.current += 1;
      setWatches([]);
      setWatchesError(null);
      setWatchesLastRefreshedAt(null);
      return;
    }

    const generation = ++watchRefreshGeneration.current;
    setWatchesLoading(true);
    setWatchesError(null);
    try {
      const [token, catalog] = await Promise.all([getToken(), listCatalogTerms()]);
      const next = await loadWatchlistItems(token, { term: catalog.currentTerm });
      if (generation !== watchRefreshGeneration.current) {
        return;
      }
      setWatches(next);
      setWatchesLastRefreshedAt(new Date().toISOString());
    } catch (error) {
      if (generation !== watchRefreshGeneration.current) {
        return;
      }
      setWatchesError(error instanceof Error ? error.message : "Failed to load watchlist");
    } finally {
      if (generation === watchRefreshGeneration.current) {
        setWatchesLoading(false);
      }
    }
  }, [getToken, isSignedIn]);

  useEffect(() => {
    if (!isLoaded) return;
    if (!isSignedIn) {
      setProfile(null);
      watchRefreshGeneration.current += 1;
      setWatches([]);
      setWatchesError(null);
      setWatchesLastRefreshedAt(null);
      return;
    }
    void refreshProfile();
    void refreshWatches();
  }, [isLoaded, isSignedIn, refreshProfile, refreshWatches]);

  const addLocalWatch = useCallback((item: WatchlistItem) => {
    setWatches((current) => {
      if (current.some((watch) => watch.courseId === item.courseId && watch.term === item.term)) {
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
      watchesLoading,
      watchesError,
      watchesLastRefreshedAt,
      refreshWatches,
      addLocalWatch,
    }),
    [
      addLocalWatch,
      profile,
      profileError,
      profileLoading,
      refreshProfile,
      refreshWatches,
      watches,
      watchesError,
      watchesLastRefreshedAt,
      watchesLoading,
    ],
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
