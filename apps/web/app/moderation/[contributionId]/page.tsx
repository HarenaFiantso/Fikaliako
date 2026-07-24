import { getTranslations } from 'next-intl/server';

import { ContributionDetail } from '@/components/moderation/contribution-detail';
import { ModerationGuard } from '@/components/moderation/moderation-guard';
import { SiteHeader } from '@/components/site-header';

export async function generateMetadata() {
  const t = await getTranslations('moderation.detail');
  return { title: t('title'), robots: { index: false } };
}

export default async function ContributionPage({
  params,
}: {
  params: Promise<{ contributionId: string }>;
}) {
  const { contributionId } = await params;

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-8 sm:px-6">
        <ModerationGuard>
          <ContributionDetail contributionId={contributionId} />
        </ModerationGuard>
      </main>
    </div>
  );
}
