import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { StatusDot } from "../components/StatusDot";
import { listCatalogTerms } from "../lib/api";
import { useAppData } from "../lib/AppDataProvider";
import { formatWatchingSince } from "../lib/format";

export function WatchlistPage() {
  const { watches } = useAppData();
  const [termLabel, setTermLabel] = useState("Current term");
  const [termCode, setTermCode] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    void listCatalogTerms(controller.signal)
      .then((response) => {
        const current =
          response.terms.find((term) => term.code === response.currentTerm) ?? response.terms[0];
        if (current) {
          setTermLabel(current.label);
          setTermCode(current.code);
        }
      })
      .catch(() => {
        // Keep fallback label if catalog is unreachable.
      });
    return () => controller.abort();
  }, []);

  return (
    <div>
      <section className="flex items-end justify-between px-18 pb-10 pt-18">
        <div className="flex flex-col gap-3.5">
          <p className="text-xs font-medium uppercase tracking-[0.08em] text-muted">
            {termLabel}
            {termCode ? ` · ${termCode}` : ""}
          </p>
          <h1 className="text-[64px] font-medium leading-16 tracking-[-0.03em] text-ink">
            Watching {watches.length} course{watches.length === 1 ? "" : "s"}
          </h1>
        </div>
        <Link
          to="/watchlist/new"
          className="rounded-sm bg-ink px-5 py-[13px] text-sm font-medium leading-[18px] text-white"
        >
          Add a watch
        </Link>
      </section>

      <div className="mx-18 flex items-center gap-6 border-b border-ink pb-3">
        <div className="w-2.5 shrink-0" />
        <div className="w-30 shrink-0 text-xs font-medium uppercase tracking-[0.08em] text-muted">
          Course
        </div>
        <div className="grow text-xs font-medium uppercase tracking-[0.08em] text-muted">Title</div>
        <div className="w-25 shrink-0 text-right text-xs font-medium uppercase tracking-[0.08em] text-muted">
          Open seats
        </div>
        <div className="w-25 shrink-0 text-right text-xs font-medium uppercase tracking-[0.08em] text-muted">
          Waitlist
        </div>
        <div className="w-32_5 shrink-0 text-right text-xs font-medium uppercase tracking-[0.08em] text-muted">
          Watching since
        </div>
      </div>

      {watches.length === 0 ? (
        <p className="mx-18 py-10 text-muted">
          No courses yet.{" "}
          <Link to="/watchlist/new" className="underline">
            Add a watch
          </Link>{" "}
          to get started.
        </p>
      ) : (
        watches.map((watch) => (
          <div
            key={watch.courseId}
            className="mx-18 flex items-center gap-6 border-b border-line py-[26px]"
          >
            <StatusDot tone={watch.seatsOpen ? "open" : "watching"} />
            <div className="w-30 shrink-0 font-mono text-[15px] font-medium leading-[18px] text-ink">
              {watch.courseId}
            </div>
            <div className="flex grow items-center gap-3">
              <span className="text-[17px] leading-[22px] tracking-[-0.01em] text-ink">
                {watch.title}
              </span>
              {watch.seatsOpen ? (
                <span className="rounded-[3px] border border-open px-[7px] py-[3px] text-[11px] font-semibold uppercase tracking-[0.08em] text-open">
                  Seats open
                </span>
              ) : null}
            </div>
            <div
              className={`w-25 shrink-0 text-right font-mono text-[17px] leading-[22px] ${
                watch.seatsOpen ? "font-semibold text-open" : "text-ink"
              }`}
            >
              {watch.openSeats}
            </div>
            <div className="w-25 shrink-0 text-right font-mono text-[17px] leading-[22px] text-muted">
              {watch.waitlist}
            </div>
            <div className="w-32_5 shrink-0 text-right text-sm leading-[18px] text-muted">
              {formatWatchingSince(watch.watchingSince)}
            </div>
          </div>
        ))
      )}

      <div className="mx-18 mt-7 flex items-center gap-2">
        <StatusDot tone="live" size="sm" />
        <p className="text-[13px] leading-4 text-muted">
          Checked against UCSD Class Planner just now · refreshes every 2 minutes
        </p>
      </div>
    </div>
  );
}
