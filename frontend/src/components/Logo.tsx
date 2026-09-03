import { Link } from "react-router-dom";

export function Logo({ inverted = false }: { inverted?: boolean }) {
  return (
    <Link to="/" className="flex items-center gap-2.5">
      <span className="size-3 shrink-0 rounded-sm bg-signal" />
      <span
        className={`text-[15px] font-semibold leading-[18px] tracking-[-0.01em] ${
          inverted ? "text-white" : "text-ink"
        }`}
      >
        Tritonwatch
      </span>
    </Link>
  );
}
