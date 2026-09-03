import { useAuth } from "@clerk/react";
import { Link } from "react-router-dom";
import { MarketingNav } from "../components/AppNav";
import { StatusDot } from "../components/StatusDot";

export function LandingPage() {
  const { isSignedIn } = useAuth();
  const startPath = isSignedIn ? "/watchlist" : "/sign-in";

  return (
    <div className="min-h-screen bg-white pb-18">
      <MarketingNav />

      <section className="mx-auto w-full max-w-[1180px] px-18 pt-[50px]">
        <h1 className="text-[112px] font-medium leading-[104px] tracking-[-0.045em] text-ink">
          mfw
        </h1>
        <p className="text-[32px] font-medium leading-[104px] tracking-[-0.045em] text-muted">
          you don't wanna waitlist cuz you don't have enough units
        </p>
      </section>

      <section className="mx-auto flex w-full max-w-[1180px] flex-col items-start gap-16 px-18 pt-11">
        <p className="w-[470px] shrink-0 text-[19px] leading-[30px] text-muted">
          tritonwatch checks the ucsd class planner every two minutes and emails or texts you the
          moment a seat opens in a course you’re waiting on
        </p>
        <div className="flex items-center gap-5">
          <Link
            to={startPath}
            className="rounded-sm bg-ink px-7 py-[17px] text-base font-medium leading-5 text-white"
          >
            Watch a course
          </Link>
          {!isSignedIn ? (
            <Link to="/sign-in" className="text-sm leading-[18px] text-muted">
              Sign in with your UCSD email
            </Link>
          ) : (
            <Link to="/watchlist" className="text-sm leading-[18px] text-muted">
              Go to your watchlist
            </Link>
          )}
        </div>
      </section>

      <div className="mx-auto mt-[110px] flex w-full max-w-[720px] items-center gap-7 rounded-md border border-line px-[30px] py-[26px]">
        <StatusDot tone="open" />
        <div className="flex grow flex-col gap-1.5">
          <p className="text-[17px] font-medium leading-[22px] tracking-[-0.01em] text-ink">
            CSE 100 just opened — 6 seats
          </p>
          <p className="text-sm leading-[18px] text-muted">
            Advanced Data Structures · Fall 2026 · sent to daniel@ucsd.edu
          </p>
        </div>
        <span className="shrink-0 font-mono text-[13px] leading-4 text-muted">2:14 AM</span>
      </div>

      <section className="mx-auto mt-[130px] flex w-full max-w-[1180px] items-end justify-between rounded-md bg-signal px-15 py-14">
        <div className="flex w-[600px] shrink-0 flex-col gap-3.5">
          <h2 className="text-5xl font-medium leading-[50px] tracking-[-0.035em] text-ink">
            Stop refreshing TSS (🫩)
          </h2>
          <p className="text-[17px] leading-[27px] text-signal-ink">
            Built by a UCSD student, for UCSD students.
          </p>
        </div>
        <Link
          to={startPath}
          className="rounded-sm bg-ink px-7 py-[17px] text-base font-medium leading-5 text-white"
        >
          Start watching
        </Link>
      </section>

      <footer className="mx-auto mt-20 flex w-full max-w-[1180px] items-center justify-between border-t border-line px-18 pt-[26px]">
        <p className="text-[13px] leading-4 text-muted">
          Tritonwatch is not affiliated with UC San Diego.
        </p>
        <div className="flex items-center gap-[26px] text-[13px] leading-4 text-muted">
          <a href="#">Privacy</a>
          <a href="#">SMS terms</a>
          <a href="https://github.com/x0ba/tritonwatch" target="_blank" rel="noreferrer">
            GitHub
          </a>
        </div>
      </footer>
    </div>
  );
}
