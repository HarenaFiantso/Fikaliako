import type { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';

import { SignedInGuard } from '@/components/auth/signed-in-guard';

import { ProposeForm } from '@/components/establishments/propose-form';
import { SiteHeader } from '@/components/site-header';

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations('propose');
  return { title: t('title'), robots: { index: false } };
}

export default function ProposePage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-8 sm:px-6">
        <SignedInGuard>
          <ProposeForm />
        </SignedInGuard>
      </main>
    </div>
  );
}
