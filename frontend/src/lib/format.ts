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
