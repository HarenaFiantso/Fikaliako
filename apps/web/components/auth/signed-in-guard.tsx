'use client';

import { useEffect } from 'react';

import { useRouter } from 'next/navigation';

import { useSession } from '@/hooks/use-session';

/**
 * Sends anonymous visitors to login. The API is the real enforcer — this only
 * spares them a form they cannot submit.
 */
export function SignedInGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { isAuthenticated, hydrated } = useSession();

  useEffect(() => {
    if (hydrated && !isAuthenticated) router.replace('/login');
  }, [hydrated, isAuthenticated, router]);

  if (!hydrated || !isAuthenticated) {
    return (
      <div className="space-y-3" aria-busy>
        <div className="bg-muted h-9 w-64 animate-pulse rounded-lg" />
        <div className="bg-muted h-64 animate-pulse rounded-2xl" />
      </div>
    );
  }

  return <>{children}</>;
}
