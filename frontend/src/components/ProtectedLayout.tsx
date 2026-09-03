import { useAuth0 } from "@auth0/auth0-react";
import { Navigate, Outlet } from "react-router-dom";
import { AppNav } from "./AppNav";

export function ProtectedLayout() {
  const { isAuthenticated, isLoading } = useAuth0();

  if (isLoading) {
    return <div className="flex min-h-screen items-center justify-center text-muted">Loading…</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/sign-in" replace />;
  }

  return (
    <div className="min-h-screen bg-white pb-20">
      <AppNav />
      <Outlet />
    </div>
  );
}
