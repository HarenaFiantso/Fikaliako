'use client';

import { useTranslations } from 'next-intl';
import Link from 'next/link';

import { Button } from '@/components/ui/button';

import { BrandLogo } from '@/components/brand-logo';
import { LocaleSwitcher } from '@/components/locale-switcher';
import { ThemeToggle } from '@/components/theme-toggle';
import { UserMenu } from '@/components/user-menu';

import { useSession } from '@/hooks/use-session';

export function SiteHeader() {
  const t = useTranslations('header');
  const { user, isAuthenticated, hydrated } = useSession();

  return (
    <header className="border-border/60 bg-background/80 sticky top-0 z-40 border-b backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
        <BrandLogo />
        <div className="flex items-center gap-1.5">
          <LocaleSwitcher />
          <ThemeToggle />
          {!hydrated ? (
            <div className="bg-muted h-9 w-36 animate-pulse rounded-lg" />
          ) : isAuthenticated && user ? (
            <UserMenu user={user} />
          ) : (
            <>
              <Button variant="ghost" size="sm" asChild className="hidden sm:inline-flex">
                <Link href="/login">{t('login')}</Link>
              </Button>
              <Button size="sm" asChild>
                <Link href="/register">{t('register')}</Link>
              </Button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
