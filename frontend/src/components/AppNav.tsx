import { useAuth, useClerk, useUser } from "@clerk/react";
import { useEffect, useId, useRef, useState } from "react";
import { Link, NavLink } from "react-router-dom";
import { Logo } from "./Logo";
import { shortDisplayName } from "../lib/format";
import { useAppData } from "../lib/AppDataProvider";

export function AppNav() {
  const { user } = useUser();
  const { profile } = useAppData();

  const name = shortDisplayName(
    profile?.displayName ?? user?.fullName ?? undefined,
    profile?.email.value ?? user?.primaryEmailAddress?.emailAddress,
  );

  return (
    <header className="flex items-center justify-between border-b border-line px-18 py-[26px]">
      <Logo />
      <UserMenu name={name} imageUrl={user?.imageUrl} />
    </header>
  );
}

function UserMenu({ name, imageUrl }: { name: string; imageUrl?: string }) {
  const { signOut } = useClerk();
  const menuId = useId();
  const rootRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!open) return;

    function onPointerDown(event: PointerEvent) {
      if (rootRef.current?.contains(event.target as Node)) return;
      setOpen(false);
    }

    function onKeyDown(event: KeyboardEvent) {
      if (event.key !== "Escape") return;
      setOpen(false);
      buttonRef.current?.focus();
    }

    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  return (
    <div ref={rootRef} className="relative">
      <button
        ref={buttonRef}
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={menuId}
        onClick={() => setOpen((current) => !current)}
        className="flex items-center gap-[9px] text-sm leading-[18px] text-ink"
      >
        {imageUrl ? (
          <img src={imageUrl} alt="" className="size-[26px] shrink-0 rounded-full object-cover" />
        ) : (
          <span className="size-[26px] shrink-0 rounded-full bg-ink" />
        )}
        {name}
      </button>
      {open ? (
        <div
          id={menuId}
          role="menu"
          className="absolute right-0 z-10 mt-2 min-w-[212px] border border-line bg-white py-1.5"
        >
          <NavLink
            role="menuitem"
            to="/notifications"
            onClick={() => setOpen(false)}
            className={({ isActive }) =>
              `block px-3.5 py-2 text-sm leading-[18px] hover:bg-ink/[0.03] ${
                isActive ? "text-ink" : "text-muted"
              }`
            }
          >
            Notification settings
          </NavLink>
          <button
            role="menuitem"
            type="button"
            onClick={() => void signOut({ redirectUrl: "/" })}
            className="block w-full px-3.5 py-2 text-left text-sm leading-[18px] text-ink hover:bg-ink/[0.03]"
          >
            Log out
          </button>
        </div>
      ) : null}
    </div>
  );
}

export function MarketingNav() {
  const { isSignedIn } = useAuth();

  return (
    <header className="flex items-center justify-between px-18 py-[26px]">
      <Logo />
      <div className="flex items-center gap-7">
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
