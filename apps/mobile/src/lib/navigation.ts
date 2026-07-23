import type { Href, useRouter } from 'expo-router';

type Router = ReturnType<typeof useRouter>;

/**
 * Pops the current screen, or lands on `fallback` when there is no history —
 * a deep link, a double-tap, or a deferred dismissal racing manual
 * navigation would otherwise drop GO_BACK with a dev warning.
 */
export function safeBack(router: Router, fallback: Href = '/'): void {
  if (router.canGoBack()) {
    router.back();
  } else {
    router.replace(fallback);
  }
}
