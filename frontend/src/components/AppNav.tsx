import { useAuth0 } from "@auth0/auth0-react";
import { Link, NavLink } from "react-router-dom";
import { Logo } from "./Logo";
import { shortDisplayName } from "../lib/format";
import { useAppData } from "../lib/AppDataProvider";

export function AppNav() {
  const { user, logout } = useAuth0();
  const { profile } = useAppData();

  const name = shortDisplayName(
    profile?.displayName ?? user?.name,
    profile?.email.value ?? user?.email,
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
          onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}
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
  const { isAuthenticated, loginWithRedirect } = useAuth0();

  return (
    <header className="flex items-center justify-between px-18 py-[26px]">
      <Logo />
      <div className="flex items-center gap-7">
        <a href="#how-it-works" className="text-sm leading-[18px] text-muted">
          How it works
        </a>
        {isAuthenticated ? (
          <Link to="/watchlist" className="text-sm leading-[18px] text-ink">
            Watchlist
          </Link>
        ) : (
          <button
            type="button"
            onClick={() => loginWithRedirect()}
            className="text-sm leading-[18px] text-ink"
          >
            Log in
          </button>
        )}
        <Link
          to={isAuthenticated ? "/watchlist" : "/sign-in"}
          className="rounded-sm bg-ink px-[18px] py-[11px] text-sm font-medium leading-[18px] text-white"
        >
          Start watching
        </Link>
      </div>
    </header>
  );
}
