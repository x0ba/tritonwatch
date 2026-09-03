import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { StatusDot } from "../components/StatusDot";
import { deleteWatchRequest } from "../lib/api";
import { useAppData, WATCHLIST_REFRESH_INTERVAL_MS } from "../lib/AppDataProvider";
import { formatCheckedAt, formatWatchingSince } from "../lib/format";
import { useAccessToken } from "../lib/useAccessToken";

export function WatchlistPage() {
  const { getToken } = useAccessToken();
  const {
    watches,
    watchesLoading,
    watchesError,
    watchesLastRefreshedAt,
    refreshWatches,
    removeLocalWatch,
  } = useAppData();
  const [checkedLabel, setCheckedLabel] = useState(() => formatCheckedAt(watchesLastRefreshedAt));
  const [removingId, setRemovingId] = useState<string | null>(null);
  const [removeError, setRemoveError] = useState<string | null>(null);

  useEffect(() => {
    void refreshWatches();

    const intervalId = window.setInterval(() => {
      void refreshWatches();
    }, WATCHLIST_REFRESH_INTERVAL_MS);

    const onVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        void refreshWatches();
      }
    };
    document.addEventListener("visibilitychange", onVisibilityChange);

    return () => {
      window.clearInterval(intervalId);
      document.removeEventListener("visibilitychange", onVisibilityChange);
    };
  }, [refreshWatches]);

  useEffect(() => {
    const updateLabel = () => setCheckedLabel(formatCheckedAt(watchesLastRefreshedAt));
    updateLabel();
    const intervalId = window.setInterval(updateLabel, 15_000);
    return () => window.clearInterval(intervalId);
  }, [watchesLastRefreshedAt]);

  async function handleRemove(watchRequestId: string) {
    setRemovingId(watchRequestId);
    setRemoveError(null);
    try {
      const token = await getToken();
      await deleteWatchRequest(token, watchRequestId);
      removeLocalWatch(watchRequestId);
    } catch (error) {
      setRemoveError(error instanceof Error ? error.message : "Could not remove watch");
    } finally {
      setRemovingId(null);
    }
  }

  const showInitialLoading =
    watchesLoading && watches.length === 0 && !watchesLastRefreshedAt && !watchesError;

  return (
    <div>
      <section className="flex items-end justify-between px-18 pb-10 pt-18">
        <div className="flex flex-col gap-3.5">
          <p className="text-xs font-medium uppercase tracking-[0.08em] text-muted">Watchlist</p>
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
        <div className="w-16 shrink-0 text-xs font-medium uppercase tracking-[0.08em] text-muted">
          Term
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
        <div className="w-20 shrink-0" />
      </div>

      {watchesError ? <p className="mx-18 py-6 text-sm text-red-700">{watchesError}</p> : null}
      {removeError ? <p className="mx-18 py-6 text-sm text-red-700">{removeError}</p> : null}

      {showInitialLoading ? (
        <p className="mx-18 py-10 text-muted">Loading your watchlist…</p>
      ) : watches.length === 0 ? (
        <p className="mx-18 py-10 text-muted">
          No courses yet.{" "}
          <Link to="/watchlist/new" className="underline">
            Add a watch
          </Link>{" "}
          to get started.
        </p>
      ) : (
        watches.map((watch) => {
          const watchId = watch.id;
          return (
            <div
              key={watchId ?? `${watch.term}:${watch.courseId}`}
              className="mx-18 flex items-center gap-6 border-b border-line py-[26px]"
            >
              <StatusDot tone={watch.seatsOpen ? "open" : "watching"} />
              <div className="w-30 shrink-0 font-mono text-[15px] font-medium leading-[18px] text-ink">
                {watch.courseId}
              </div>
              <div className="w-16 shrink-0 font-mono text-[15px] font-medium leading-[18px] text-muted">
                {watch.term}
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
              <div className="w-20 shrink-0 text-right">
                {watchId ? (
                  <button
                    type="button"
                    onClick={() => void handleRemove(watchId)}
                    disabled={removingId === watchId}
                    aria-label={`Remove ${watch.courseId} from watchlist`}
                    className="text-sm leading-[18px] text-muted underline disabled:cursor-not-allowed disabled:no-underline disabled:opacity-40"
                  >
                    {removingId === watchId ? "Removing…" : "Remove"}
                  </button>
                ) : null}
              </div>
            </div>
          );
        })
      )}

      <div className="mx-18 mt-7 flex items-center gap-2">
        <StatusDot tone="live" size="sm" />
        <p className="text-[13px] leading-4 text-muted">
          Checked against UCSD Class Planner {checkedLabel} · refreshes every 2 minutes
        </p>
      </div>
    </div>
  );
}
