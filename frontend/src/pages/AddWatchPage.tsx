import { useEffect, useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../components/Button";
import { createWatchRequest, listCatalogTerms, searchCatalogCourses } from "../lib/api";
import { useAppData } from "../lib/AppDataProvider";
import { useAccessToken } from "../lib/useAccessToken";
import type { CatalogCourse, TermOption } from "../lib/types";

export function AddWatchPage() {
  const navigate = useNavigate();
  const { getToken } = useAccessToken();
  const { profile, addLocalWatch, refreshWatches } = useAppData();
  const [terms, setTerms] = useState<TermOption[]>([]);
  const [term, setTerm] = useState("");
  const [query, setQuery] = useState("");
  const [matches, setMatches] = useState<CatalogCourse[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [selected, setSelected] = useState<CatalogCourse | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    void listCatalogTerms(controller.signal)
      .then((response) => {
        setTerms(response.terms);
        setTerm((current) => current || response.currentTerm || response.terms[0]?.code || "");
      })
      .catch((err) => {
        if (controller.signal.aborted) return;
        setSearchError(err instanceof Error ? err.message : "Could not load terms");
      });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (!term) {
      setMatches([]);
      return;
    }

    const trimmed = query.trim();
    if (!trimmed) {
      setMatches([]);
      setSearchLoading(false);
      setSearchError(null);
      return;
    }

    const controller = new AbortController();
    const handle = window.setTimeout(() => {
      setSearchLoading(true);
      setSearchError(null);
      void searchCatalogCourses({
        term,
        query: trimmed,
        limit: 25,
        signal: controller.signal,
      })
        .then((courses) => {
          setMatches(courses);
        })
        .catch((err) => {
          if (controller.signal.aborted) return;
          setMatches([]);
          setSearchError(err instanceof Error ? err.message : "Could not search catalog");
        })
        .finally(() => {
          if (!controller.signal.aborted) {
            setSearchLoading(false);
          }
        });
    }, 200);

    return () => {
      controller.abort();
      window.clearTimeout(handle);
    };
  }, [query, term]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!selected || !term) return;

    setSubmitting(true);
    setError(null);
    try {
      const token = await getToken();
      const created = await createWatchRequest(token, {
        courseId: selected.courseId,
        term,
      });
      addLocalWatch({
        id: created.id,
        courseId: selected.courseId,
        term,
        title: selected.title,
        openSeats: selected.openSeats,
        waitlist: selected.waitlist,
        watchingSince: created.createdAt,
        seatsOpen: selected.openSeats > 0,
      });
      void refreshWatches();
      void navigate("/watchlist");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not create watch");
    } finally {
      setSubmitting(false);
    }
  }

  const emailHint = profile?.email.value ?? "your email";
  const selectedTermLabel = terms.find((option) => option.code === term)?.label ?? term;

  return (
    <form onSubmit={handleSubmit} className="pb-10">
      <section className="flex w-[820px] flex-col gap-4 px-18 pt-20">
        <p className="text-xs font-medium uppercase tracking-[0.08em] text-muted">Step 1 of 1</p>
        <h1 className="text-[56px] font-medium leading-[58px] tracking-[-0.03em] text-ink">
          Which course should we watch?
        </h1>
      </section>

      <div className="mx-18 mt-11 flex w-[820px] border-b-2 border-ink">
        <label className="mr-5 flex items-center gap-2.5 border-r border-line pr-5 pb-3.5">
          <select
            value={term}
            onChange={(e) => {
              setTerm(e.target.value);
              setSelected(null);
            }}
            className="appearance-none bg-transparent text-[17px] font-medium leading-[22px] text-ink outline-none"
          >
            {terms.length === 0 ? (
              <option value={term}>{selectedTermLabel || "Loading…"}</option>
            ) : null}
            {terms.map((option) => (
              <option key={option.code} value={option.code}>
                {option.label}
              </option>
            ))}
          </select>
          <span className="text-[17px] leading-[22px] text-ink" aria-hidden>
            ▾
          </span>
        </label>
        <input
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setSelected(null);
          }}
          className="grow bg-transparent pb-3.5 font-mono text-[17px] font-medium leading-[22px] text-ink outline-none"
          placeholder="Course code"
          aria-label="Search courses"
        />
        <span className="pb-3.5 text-[13px] leading-4 text-muted">
          {searchLoading
            ? "Searching…"
            : `${matches.length} match${matches.length === 1 ? "" : "es"}`}
        </span>
      </div>

      <div className="mx-18 w-[820px]">
        {searchError ? <p className="py-6 text-sm text-red-700">{searchError}</p> : null}
        {!searchError && query.trim() && !searchLoading && matches.length === 0 ? (
          <p className="py-6 text-sm text-muted">No courses match “{query.trim()}”.</p>
        ) : null}
        {matches.map((course) => {
          const isSelected = selected?.courseId === course.courseId;
          return (
            <button
              key={course.courseId}
              type="button"
              onClick={() => setSelected(course)}
              className={`flex w-full items-center gap-6 border-b border-line py-[22px] pr-5 pl-4 text-left ${
                isSelected
                  ? "border-l-[3px] border-l-signal bg-signal-soft"
                  : "border-l-[3px] border-l-white"
              }`}
            >
              <span className="w-30 shrink-0 font-mono text-[15px] font-medium leading-[18px] text-ink">
                {course.courseId}
              </span>
              <span className="grow text-[17px] leading-[22px] tracking-[-0.01em] text-ink">
                {course.title}
              </span>
              <span className="w-[110px] shrink-0 text-right text-sm leading-[18px] text-muted">
                {course.openSeats} seats open
              </span>
              <span className="w-[70px] shrink-0 text-right text-sm font-medium leading-[18px] text-ink">
                {isSelected ? "Selected" : ""}
              </span>
            </button>
          );
        })}
      </div>

      <div className="mx-18 mt-10 flex w-[820px] items-center gap-5">
        <Button type="submit" disabled={!selected || submitting} className="px-6 py-[15px]">
          {selected ? (submitting ? "Watching…" : `Watch ${selected.courseId}`) : "Select a course"}
        </Button>
        <p className="grow text-sm leading-[21px] text-muted">
          We’ll email {emailHint} the moment a seat opens. Already watching it? Nothing happens
          twice.
        </p>
      </div>

      {error ? <p className="mx-18 mt-4 w-[820px] text-sm text-red-700">{error}</p> : null}
    </form>
  );
}
