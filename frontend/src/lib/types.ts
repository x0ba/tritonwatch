export type ContactChannel = {
  value: string | null;
  verified: boolean;
  verifiedAt: string | null;
};

export type NotificationPreferences = {
  emailEnabled: boolean;
  smsEnabled: boolean;
  effectiveEmailEnabled: boolean;
  effectiveSmsEnabled: boolean;
};

export type UserProfile = {
  userId: string;
  displayName: string | null;
  email: ContactChannel;
  phone: ContactChannel;
  notificationPreferences: NotificationPreferences;
  status: string;
  version: number;
  createdAt: string;
  updatedAt: string;
};

export type UpdateProfileRequest = {
  displayName?: string | null;
  email?: string | null;
  phoneNumber?: string | null;
};

export type UpdateNotificationPreferencesRequest = {
  emailEnabled: boolean;
  smsEnabled: boolean;
  smsConsentAccepted?: boolean;
  smsConsentPolicyVersion?: string;
};

export type WatchRequest = {
  id: string;
  courseId: string;
  term: string;
  createdAt: string;
};

export type CreateWatchRequest = {
  courseId: string;
  term: string;
};

export type CatalogCourse = {
  courseId: string;
  title: string;
  openSeats: number;
  waitlist: number;
};

export type WatchlistItem = {
  id?: string;
  courseId: string;
  term: string;
  title: string;
  openSeats: number;
  waitlist: number;
  watchingSince: string;
  seatsOpen: boolean;
};

export type TermOption = {
  code: string;
  label: string;
};
