export function shortDisplayName(name: string | undefined, email: string | undefined): string {
  if (name) {
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
      const first = parts[0] ?? "";
      const last = parts[parts.length - 1] ?? "";
      return `${first} ${last.charAt(0)}.`;
    }
    return name;
  }
  if (email) {
    return email.split("@")[0] ?? "Account";
  }
  return "Account";
}

export function formatVerifiedLabel(verified: boolean, verifiedAt: string | null): string {
  if (!verified || !verifiedAt) {
    return "Not verified yet";
  }
  const date = new Date(verifiedAt);
  return `Verified ${date.toLocaleDateString("en-US", { month: "short", day: "numeric" })}`;
}

export function formatWatchingSince(iso: string): string {
  return new Date(iso).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
  });
}

export function formatCheckedAt(iso: string | null, now = Date.now()): string {
  if (!iso) {
    return "not yet";
  }

  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) {
    return "not yet";
  }

  const deltaMs = Math.max(0, now - then);
  if (deltaMs < 15_000) {
    return "just now";
  }
  if (deltaMs < 60_000) {
    return `${Math.floor(deltaMs / 1000)}s ago`;
  }
  if (deltaMs < 3_600_000) {
    const minutes = Math.floor(deltaMs / 60_000);
    return `${minutes} min${minutes === 1 ? "" : "s"} ago`;
  }

  return new Date(iso).toLocaleTimeString("en-US", {
    hour: "numeric",
    minute: "2-digit",
  });
}
