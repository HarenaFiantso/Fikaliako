export const locales = ['fr', 'mg', 'en'] as const;

export type Locale = (typeof locales)[number];

export const defaultLocale: Locale = 'fr';

export const LOCALE_COOKIE = 'NEXT_LOCALE';

export function isLocale(value: unknown): value is Locale {
  return typeof value === 'string' && (locales as readonly string[]).includes(value);
}

/** Locales the API accepts for a user profile (contract: UserProfile.locale). */
export const apiLocales = ['fr', 'mg'] as const;

export type ApiLocale = (typeof apiLocales)[number];

/** Maps a UI locale to the nearest API profile locale (`en` has no API equivalent yet). */
export function toApiLocale(locale: Locale): ApiLocale {
  return locale === 'mg' ? 'mg' : 'fr';
}
