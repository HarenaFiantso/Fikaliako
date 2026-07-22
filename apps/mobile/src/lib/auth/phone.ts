const E164_PATTERN = /^\+[1-9][0-9]{7,14}$/;
const MALAGASY_LOCAL_PATTERN = /^0[0-9]{9}$/;

export function normalizePhone(input: string): string | null {
  const cleaned = input.replace(/[\s().-]/g, '');
  let candidate = cleaned;
  if (cleaned.startsWith('00')) {
    candidate = `+${cleaned.slice(2)}`;
  } else if (MALAGASY_LOCAL_PATTERN.test(cleaned)) {
    candidate = `+261${cleaned.slice(1)}`;
  }
  return E164_PATTERN.test(candidate) ? candidate : null;
}

export function formatPhone(phone: string): string {
  const match = /^\+261(3[0-9])([0-9]{2})([0-9]{3})([0-9]{2})$/.exec(phone);
  if (!match) return phone;
  return `0${match[1]} ${match[2]} ${match[3]} ${match[4]}`;
}
