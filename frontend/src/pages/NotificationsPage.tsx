import { useEffect, useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../components/Button";
import { Toggle } from "../components/Toggle";
import {
  deleteMe,
  requestEmailVerification,
  requestPhoneVerification,
  updateNotificationPreferences,
  upsertMe,
} from "../lib/api";
import { useAppData } from "../lib/AppDataProvider";
import { formatVerifiedLabel } from "../lib/format";
import { useAccessToken } from "../lib/useAccessToken";

const SMS_CONSENT_POLICY_VERSION = "2.1";

export function NotificationsPage() {
  const navigate = useNavigate();
  const { getToken } = useAccessToken();
  const { profile, setProfile, refreshProfile } = useAppData();

  const [emailEnabled, setEmailEnabled] = useState(true);
  const [smsEnabled, setSmsEnabled] = useState(false);
  const [smsConsent, setSmsConsent] = useState(false);
  const [phoneDraft, setPhoneDraft] = useState("");
  const [editingPhone, setEditingPhone] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!profile) return;
    setEmailEnabled(profile.notificationPreferences.emailEnabled);
    setSmsEnabled(profile.notificationPreferences.smsEnabled);
    setPhoneDraft(profile.phone.value ?? "");
    setSmsConsent(profile.notificationPreferences.smsEnabled);
  }, [profile]);

  async function handleSave(event: FormEvent) {
    event.preventDefault();
    if (!profile) return;

    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const token = await getToken("update:user-profile");

      let next = profile;
      if (editingPhone || phoneDraft !== (profile.phone.value ?? "")) {
        next = await upsertMe(token, {
          displayName: profile.displayName,
          email: profile.email.value,
          phoneNumber: phoneDraft || null,
        });
        setEditingPhone(false);
      }

      next = await updateNotificationPreferences(token, {
        emailEnabled,
        smsEnabled,
        ...(smsEnabled
          ? {
              smsConsentAccepted: smsConsent,
              smsConsentPolicyVersion: SMS_CONSENT_POLICY_VERSION,
            }
          : {}),
      });

      setProfile(next);
      setMessage("Preferences saved.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save preferences");
    } finally {
      setSaving(false);
    }
  }

  async function handleVerifyEmail() {
    setError(null);
    setMessage(null);
    try {
      const token = await getToken("update:user-profile");
      await requestEmailVerification(token);
      setMessage("Verification code sent to your email.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not send email code");
    }
  }

  async function handleVerifyPhone() {
    setError(null);
    setMessage(null);
    try {
      const token = await getToken("update:user-profile");
      if (phoneDraft && phoneDraft !== (profile?.phone.value ?? "")) {
        await upsertMe(token, {
          displayName: profile?.displayName ?? null,
          email: profile?.email.value ?? null,
          phoneNumber: phoneDraft,
        });
        await refreshProfile();
      }
      await requestPhoneVerification(token);
      setMessage("Verification code sent to your phone.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not send SMS code");
    }
  }

  async function handleDelete() {
    if (!window.confirm("Delete your account and stop all alerts? This cannot be undone.")) {
      return;
    }
    try {
      const token = await getToken("update:user-profile");
      await deleteMe(token);
      void navigate("/");
      window.location.reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not delete account");
    }
  }

  if (!profile) {
    return <div className="px-18 pt-20 text-muted">Loading account settings…</div>;
  }

  return (
    <form onSubmit={handleSave} className="pb-10">
      <section className="flex w-[820px] flex-col gap-3.5 px-18 pb-12 pt-20">
        <p className="text-xs font-medium uppercase tracking-[0.08em] text-muted">Account</p>
        <h1 className="text-[56px] font-medium leading-[58px] tracking-[-0.03em] text-ink">
          How we reach you
        </h1>
      </section>

      <div className="mx-18 flex w-[820px] items-start gap-6 border-t border-ink border-b border-b-line py-7">
        <div className="flex grow flex-col gap-[7px]">
          <p className="text-xl font-medium leading-6 tracking-[-0.01em] text-ink">Email</p>
          <p className="font-mono text-sm leading-[18px] text-muted">
            {profile.email.value ?? "No email on file"}
          </p>
          {!profile.email.verified && profile.email.value ? (
            <button
              type="button"
              onClick={() => void handleVerifyEmail()}
              className="mt-1 self-start text-[13px] text-muted underline"
            >
              Send verification code
            </button>
          ) : null}
        </div>
        <div className="w-[150px] shrink-0 pt-1">
          <p
            className={`text-[13px] font-medium leading-4 ${
              profile.email.verified ? "text-open" : "text-signal-deep"
            }`}
          >
            {formatVerifiedLabel(profile.email.verified, profile.email.verifiedAt)}
          </p>
        </div>
        <Toggle
          checked={emailEnabled}
          onChange={setEmailEnabled}
          aria-label="Enable email alerts"
        />
      </div>

      <div className="mx-18 flex w-[820px] items-start gap-6 border-b border-line py-7">
        <div className="flex grow flex-col gap-[7px]">
          <p className="text-xl font-medium leading-6 tracking-[-0.01em] text-ink">Text message</p>
          {editingPhone ? (
            <input
              value={phoneDraft}
              onChange={(e) => setPhoneDraft(e.target.value)}
              placeholder="+18585550123"
              className="max-w-xs border-b border-ink bg-transparent py-1 font-mono text-sm outline-none"
            />
          ) : (
            <button
              type="button"
              onClick={() => setEditingPhone(true)}
              className="self-start font-mono text-sm leading-[18px] text-muted"
            >
              {phoneDraft || "Add a phone number"}
            </button>
          )}
          {!profile.phone.verified && phoneDraft ? (
            <button
              type="button"
              onClick={() => void handleVerifyPhone()}
              className="mt-1 self-start text-[13px] text-muted underline"
            >
              Send verification code
            </button>
          ) : null}
        </div>
        <div className="w-[150px] shrink-0 pt-1">
          <p
            className={`text-[13px] font-medium leading-4 ${
              profile.phone.verified ? "text-open" : "text-signal-deep"
            }`}
          >
            {formatVerifiedLabel(profile.phone.verified, profile.phone.verifiedAt)}
          </p>
        </div>
        <Toggle checked={smsEnabled} onChange={setSmsEnabled} aria-label="Enable SMS alerts" />
      </div>

      {smsEnabled ? (
        <label className="mx-18 mt-7 flex w-[600px] items-start gap-3.5 border-l-[3px] border-signal bg-signal-soft px-[22px] py-5">
          <input
            type="checkbox"
            checked={smsConsent}
            onChange={(e) => setSmsConsent(e.target.checked)}
            className="mt-0.5 size-[17px] shrink-0 rounded-[3px] border-[1.5px] border-ink accent-ink"
            required={smsEnabled}
          />
          <span className="grow text-sm leading-[22px] text-ink">
            I agree to receive automated seat alerts by text. Message and data rates may apply.
            Reply STOP to opt out. Policy v{SMS_CONSENT_POLICY_VERSION}
          </span>
        </label>
      ) : null}

      <div className="mx-18 mt-11 flex w-[820px] items-center gap-6 border-t border-line pt-7">
        <Button type="submit" disabled={saving} className="px-6 py-[15px]">
          {saving ? "Saving…" : "Save preferences"}
        </Button>
        <button
          type="button"
          onClick={() => void handleDelete()}
          className="grow text-left text-sm leading-[18px] text-muted underline"
        >
          Delete my account and stop all alerts
        </button>
      </div>

      {message ? <p className="mx-18 mt-4 w-[820px] text-sm text-open">{message}</p> : null}
      {error ? <p className="mx-18 mt-4 w-[820px] text-sm text-red-700">{error}</p> : null}
    </form>
  );
}
