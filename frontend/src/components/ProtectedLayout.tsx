import { useAuth } from "@clerk/react";
import { Navigate, Outlet } from "react-router-dom";
import { AppNav } from "./AppNav";

export function ProtectedLayout() {
  const { isLoaded, isSignedIn } = useAuth();

  if (!isLoaded) {
    return <div className="flex min-h-screen items-center justify-center text-muted">Loading…</div>;
  }

  if (!isSignedIn) {
    return <Navigate to="/sign-in" replace />;
  }

  return (
    <div className="min-h-screen bg-white pb-20">
      <AppNav />
      <Outlet />
    </div>
  );
}
