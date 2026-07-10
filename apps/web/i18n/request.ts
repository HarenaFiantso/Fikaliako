import { getRequestConfig } from 'next-intl/server';
import { cookies } from 'next/headers';

import { defaultLocale, isLocale, LOCALE_COOKIE } from './config';

type Messages = Record<string, unknown>;

/**
 * Recursively overlays a partial translation onto the French base so
 * not-yet-translated keys fall back to French instead of erroring.
 */
function mergeMessages(base: Messages, overrides: Messages): Messages {
  const merged: Messages = { ...base };
  for (const [key, value] of Object.entries(overrides)) {
    const current = merged[key];
    merged[key] =
      value && typeof value === 'object' && current && typeof current === 'object'
        ? mergeMessages(current as Messages, value as Messages)
        : value;
  }
  return merged;
}

export default getRequestConfig(async () => {
  const cookieStore = await cookies();
  const requested = cookieStore.get(LOCALE_COOKIE)?.value;
  const locale = isLocale(requested) ? requested : defaultLocale;

  const base = (await import('../messages/fr.json')).default as Messages;
  const messages =
    locale === 'fr'
      ? base
      : mergeMessages(base, (await import(`../messages/${locale}.json`)).default as Messages);

  return { locale, messages };
});
