import { getTranslations } from 'next-intl/server';

import { createFikaliakoClient, type PingResponse } from '@fikaliako/api-client';

import { LandingHero } from '@/components/landing-hero';
import { SiteHeader } from '@/components/site-header';

import { API_BASE_URL } from '@/lib/api/base-url';

const api = createFikaliakoClient(API_BASE_URL);

async function fetchPing(): Promise<PingResponse | null> {
  try {
    const { data } = await api.GET('/v1/ping', { cache: 'no-store' });
    return data ?? null;
  } catch {
    return null;
  }
}

export default async function Home() {
  const [ping, t] = await Promise.all([fetchPing(), getTranslations('landing')]);

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <LandingHero />
      </main>
      <footer className="border-border/60 border-t py-5">
        <p className="text-muted-foreground mx-auto max-w-6xl px-4 text-center text-xs sm:px-6">
          {ping
            ? t('api-says', { service: ping.service, message: ping.message })
            : t('api-unreachable')}
        </p>
      </footer>
    </div>
  );
}
