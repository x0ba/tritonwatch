import { SignIn, useAuth } from "@clerk/react";
import { Navigate } from "react-router-dom";
import { Logo } from "../components/Logo";

export function SignInPage() {
  const { isLoaded, isSignedIn } = useAuth();

  if (!isLoaded) {
    return <div className="flex min-h-screen items-center justify-center text-muted">Loading…</div>;
  }

  if (isSignedIn) {
    return <Navigate to="/watchlist" replace />;
  }

  return (
    <div className="flex min-h-screen bg-white">
      <aside className="flex w-[600px] shrink-0 flex-col justify-between bg-ink px-14 py-12">
        <Logo inverted />
        <div className="flex flex-col gap-5">
          <h1 className="max-w-[488px] text-[44px] font-medium leading-12 tracking-[-0.035em] text-white">
            Watching 1,284 courses for 430 Tritons right now.
          </h1>
          <p className="max-w-[400px] text-base leading-[26px] text-faint">
            Last seat found 4 minutes ago in MATH 20C.
          </p>
        </div>
        <p className="text-[13px] leading-4 text-[#6E6E6E]">Not affiliated with UC San Diego.</p>
      </aside>

      <main className="flex grow items-center justify-center px-[110px]">
        <SignIn path="/sign-in" routing="path" withSignUp />
      </main>
    </div>
  );
}
