/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_CLERK_PUBLISHABLE_KEY: string;
  readonly VITE_WATCHLIST_API_BASE_URL: string;
  readonly VITE_USER_API_BASE_URL: string;
  readonly VITE_CATALOG_API_BASE_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
