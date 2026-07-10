const E164_PATTERN = /^\+[1-9][0-9]{7,14}$/;

/**
 * Normalizes user phone input to the E.164 form the API expects.
 * Accepts the Malagasy local format (034 12 345 67), the international
 * form with or without `+`, and `00` prefixes; returns null when the
 * result is not a plausible E.164 number.
 */
export function normalizePhone(input: string): string | null {
  const compact = input.replace(/[\s.\-()]/g, '');

  let candidate = compact;
  if (/^0[1-9][0-9]{8}$/.test(compact)) {
    candidate = `+261${compact.slice(1)}`;
  } else if (compact.startsWith('00')) {
    candidate = `+${compact.slice(2)}`;
  } else if (/^261[0-9]+$/.test(compact)) {
    candidate = `+${compact}`;
  }

  return E164_PATTERN.test(candidate) ? candidate : null;
}

/** Formats an E.164 Malagasy number back to the familiar local form for display. */
export function formatPhoneForDisplay(phone: string): string {
  const match = /^\+261([0-9]{2})([0-9]{2})([0-9]{3})([0-9]{2})$/.exec(phone);
  if (!match) return phone;
  return `0${match[1]} ${match[2]} ${match[3]} ${match[4]}`;
}
