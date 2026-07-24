'use client';

import { useEffect } from 'react';

import { useTranslations } from 'next-intl';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

import { ShieldAlert } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';

import { useSession } from '@/hooks/use-session';

import { isStaff } from '@/lib/moderation';

/**
 * Client-side gate for the moderation area: anonymous visitors go to the
 * login page, signed-in non-staff accounts get a refusal. The API enforces
 * the same rule server-side — this only shapes the experience.
 */
export function ModerationGuard({ children }: { children: React.ReactNode }) {
  const t = useTranslations('moderation.guard');
  const router = useRouter();
  const { user, isAuthenticated, hydrated } = useSession();

  useEffect(() => {
    if (hydrated && !isAuthenticated) router.replace('/login');
  }, [hydrated, isAuthenticated, router]);

  if (!hydrated || !isAuthenticated) {
    return (
      <div className="space-y-3" aria-busy>
        <div className="bg-muted h-9 w-64 animate-pulse rounded-lg" />
        <div className="bg-muted h-40 animate-pulse rounded-2xl" />
      </div>
    );
  }

  if (!isStaff(user?.role)) {
    return (
      <Card className="mx-auto max-w-md text-center">
        <CardContent className="flex flex-col items-center gap-4 py-4">
          <span className="bg-destructive/10 text-destructive flex size-12 items-center justify-center rounded-full">
            <ShieldAlert className="size-6" />
          </span>
          <div className="space-y-1">
            <p className="text-lg font-bold">{t('restricted-title')}</p>
            <p className="text-muted-foreground text-sm">{t('restricted-body')}</p>
          </div>
          <Button variant="outline" size="sm" asChild>
            <Link href="/">{t('back-home')}</Link>
          </Button>
        </CardContent>
      </Card>
    );
  }

  return <>{children}</>;
}
