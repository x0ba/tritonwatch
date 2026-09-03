type Tone = "open" | "watching" | "live";

const tones: Record<Tone, string> = {
  open: "bg-open",
  watching: "bg-signal",
  live: "bg-open",
};

export function StatusDot({ tone, size = "md" }: { tone: Tone; size?: "sm" | "md" }) {
  return (
    <span
      className={`shrink-0 rounded-[5px] ${tones[tone]} ${
        size === "sm" ? "size-1.5 rounded-[3px]" : "size-2.5"
      }`}
    />
  );
}
