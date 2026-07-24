import { getTranslations } from 'next-intl/server';

import { ModerationGuard } from '@/components/moderation/moderation-guard';
import { ModerationQueue } from '@/components/moderation/moderation-queue';
import { SiteHeader } from '@/components/site-header';

export async function generateMetadata() {
  const t = await getTranslations('moderation');
  // Internal tool: never indexed, unlike the SEO-first public pages.
  return { title: t('title'), robots: { index: false } };
}

export default function ModerationPage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">
        <ModerationGuard>
          <ModerationQueue />
        </ModerationGuard>
      </main>
    </div>
  );
}
