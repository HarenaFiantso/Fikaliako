import { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';

import { ExploreList } from '@/components/establishments/explore-list';
import { SiteHeader } from '@/components/site-header';

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations('explore');
  return { title: t('title'), description: t('tagline') };
}

export default function ExplorePage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8 sm:px-6">
        <ExploreList />
      </main>
    </div>
  );
}
