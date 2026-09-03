import { useAuth0 } from "@auth0/auth0-react";
import { useState, type FormEvent } from "react";
import { Navigate } from "react-router-dom";
import { Button } from "../components/Button";
import { Logo } from "../components/Logo";

export function SignInPage() {
  const { isAuthenticated, isLoading, loginWithRedirect } = useAuth0();
  const [email, setEmail] = useState("");

  if (isLoading) {
    return <div className="flex min-h-screen items-center justify-center text-muted">Loading…</div>;
  }

  if (isAuthenticated) {
    return <Navigate to="/watchlist" replace />;
  }

  async function handlePasswordless(event: FormEvent) {
    event.preventDefault();
    await loginWithRedirect({
      authorizationParams: {
        login_hint: email || undefined,
        connection: "email",
      },
    });
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

      <main className="flex grow flex-col justify-center px-[110px]">
        <div className="mx-auto w-full max-w-[620px]">
          <h1 className="text-[38px] font-medium leading-[42px] tracking-[-0.03em] text-ink">
            Sign in
          </h1>
          <p className="mt-3 text-base leading-[26px] text-muted">
            Use the account you want your alerts sent to.
          </p>

          <button
            type="button"
            onClick={() =>
              loginWithRedirect({
                authorizationParams: {
                  connection: "samlp",
                },
              })
            }
            className="mt-10 flex w-full items-center justify-center gap-3 rounded-sm border border-ink py-4"
          >
            <span className="size-[18px] shrink-0 rounded-full bg-signal" />
            <span className="text-base font-medium leading-5 text-ink">
              Continue with UCSD Single Sign-On
            </span>
          </button>

          <div className="mt-8 flex items-center gap-4">
            <div className="h-px grow bg-line" />
            <span className="text-xs font-medium uppercase tracking-[0.08em] text-muted">or</span>
            <div className="h-px grow bg-line" />
          </div>

          <form onSubmit={handlePasswordless} className="mt-8">
            <label className="block text-xs font-medium uppercase tracking-[0.08em] text-muted">
              Email address
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@ucsd.edu"
              className="mt-2.5 w-full border-0 border-b-2 border-ink bg-transparent py-3.5 font-mono text-base leading-5 text-ink outline-none placeholder:text-faint"
              required
            />
            <Button type="submit" className="mt-7 w-full py-[17px] text-base">
              Email me a sign-in link
            </Button>
          </form>

          <div className="mt-7 flex flex-col gap-2.5">
            <p className="text-sm leading-[23px] text-muted">
              No password to remember. The link expires in 10 minutes.
            </p>
            <p className="text-[13px] leading-[22px] text-muted">
              By continuing you agree to the Terms and Privacy Policy.
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
