'use client';

import { useEffect } from 'react';

import { useRouter } from 'next/navigation';

import { BrandPanel } from '@/components/auth/brand-panel';

import { BrandLogo } from '@/components/brand-logo';
import { LocaleSwitcher } from '@/components/locale-switcher';
import { ThemeToggle } from '@/components/theme-toggle';

import { useSession } from '@/hooks/use-session';

export function AuthShell({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useSession();
  const router = useRouter();

  useEffect(() => {
    if (isAuthenticated) router.replace('/');
  }, [isAuthenticated, router]);

  return (
    <div className="flex min-h-dvh">
      <BrandPanel />
      <div className="relative flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between p-4 sm:p-6">
          <BrandLogo className="lg:invisible" />
          <div className="flex items-center gap-1">
            <LocaleSwitcher />
            <ThemeToggle />
          </div>
        </header>
        <main className="flex flex-1 items-start justify-center px-4 pt-4 pb-16 sm:items-center sm:px-6 sm:pt-0">
          <div className="w-full max-w-md">{children}</div>
        </main>
      </div>
    </div>
  );
}
