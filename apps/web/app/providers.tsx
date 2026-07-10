'use client';

import { useEffect } from 'react';

import { ThemeProvider } from 'next-themes';

import { Toaster } from '@/components/ui/sonner';

import { useAuthStore } from '@/lib/auth/auth-store';
import { usePendingAuthStore } from '@/lib/auth/pending-auth-store';

import { QueryProvider } from './query-provider';

export function Providers({ children }: { children: React.ReactNode }) {
  useEffect(() => {
    void useAuthStore.persist.rehydrate();
    void usePendingAuthStore.persist.rehydrate();
  }, []);

  return (
    <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
      <QueryProvider>
        {children}
        <Toaster position="top-center" />
      </QueryProvider>
    </ThemeProvider>
  );
}
