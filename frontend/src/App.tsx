import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedLayout } from "./components/ProtectedLayout";
import { AddWatchPage } from "./pages/AddWatchPage";
import { LandingPage } from "./pages/LandingPage";
import { NotificationsPage } from "./pages/NotificationsPage";
import { SignInPage } from "./pages/SignInPage";
import { WatchlistPage } from "./pages/WatchlistPage";

export function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/sign-in/*" element={<SignInPage />} />
      <Route element={<ProtectedLayout />}>
        <Route path="/watchlist" element={<WatchlistPage />} />
        <Route path="/watchlist/new" element={<AddWatchPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
