import { useAuth, useClerk, useUser } from "@clerk/react";
import { Link, NavLink } from "react-router-dom";
import { Logo } from "./Logo";
import { shortDisplayName } from "../lib/format";
import { useAppData } from "../lib/AppDataProvider";

export function AppNav() {
  const { signOut } = useClerk();
  const { user } = useUser();
  const { profile } = useAppData();

  const name = shortDisplayName(
    profile?.displayName ?? user?.fullName ?? undefined,
    profile?.email.value ?? user?.primaryEmailAddress?.emailAddress,
  );

  return (
    <header className="flex items-center justify-between border-b border-line px-18 py-[26px]">
      <Logo />
      <div className="flex items-center gap-8">
        <NavLink
          to="/notifications"
          className={({ isActive }) =>
            `text-sm leading-[18px] ${isActive ? "text-ink" : "text-muted"}`
          }
        >
          Notifications
        </NavLink>
        <button
          type="button"
          onClick={() => void signOut({ redirectUrl: "/" })}
          className="flex items-center gap-[9px] text-sm leading-[18px] text-ink"
        >
          <span className="size-[26px] shrink-0 rounded-full bg-ink" />
          {name}
        </button>
      </div>
    </header>
  );
}

export function MarketingNav() {
  const { isSignedIn } = useAuth();

  return (
    <header className="flex items-center justify-between px-18 py-[26px]">
      <Logo />
      <div className="flex items-center gap-7">
        <a href="#how-it-works" className="text-sm leading-[18px] text-muted">
          How it works
        </a>
        {isSignedIn ? (
          <Link to="/watchlist" className="text-sm leading-[18px] text-ink">
            Watchlist
          </Link>
        ) : (
          <Link to="/sign-in" className="text-sm leading-[18px] text-ink">
            Log in
          </Link>
        )}
        <Link
          to={isSignedIn ? "/watchlist" : "/sign-in"}
          className="rounded-sm bg-ink px-[18px] py-[11px] text-sm font-medium leading-[18px] text-white"
        >
          Start watching
        </Link>
      </div>
    </header>
  );
}
