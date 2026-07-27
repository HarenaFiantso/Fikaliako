'use client';

import { useTranslations } from 'next-intl';
import Link from 'next/link';

import { Plus, UtensilsCrossed } from 'lucide-react';

import { FormAlert } from '@/components/auth/form-alert';

import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';

import { EstablishmentCard } from '@/components/establishments/establishment-card';

import { useEstablishments } from '@/hooks/use-establishments';

export function ExploreList() {
  const t = useTranslations('explore');
  const query = useEstablishments();
  const items = query.data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-extrabold tracking-tight">{t('title')}</h1>
          <p className="text-muted-foreground text-sm">{t('tagline')}</p>
        </div>
        <Button size="sm" asChild>
          <Link href="/propose">
            <Plus />
            <span className="hidden sm:inline">{t('add-place')}</span>
          </Link>
        </Button>
      </div>

      {query.isPending ? (
        <ul className="space-y-3" aria-busy>
          {Array.from({ length: 5 }, (_, index) => (
            <li key={index} className="bg-muted h-[76px] animate-pulse rounded-2xl" />
          ))}
        </ul>
      ) : query.isError ? (
        <div className="space-y-3">
          <FormAlert message={t('error')} />
          <Button variant="outline" size="sm" onClick={() => query.refetch()}>
            {t('retry')}
          </Button>
        </div>
      ) : items.length === 0 ? (
        <Card>
          <CardContent className="text-muted-foreground flex flex-col items-center gap-3 py-10 text-center text-sm">
            <UtensilsCrossed className="size-8 opacity-60" />
            {t('empty')}
          </CardContent>
        </Card>
      ) : (
        <>
          <ul className="space-y-3">
            {items.map((establishment) => (
              <EstablishmentCard key={establishment.id} establishment={establishment} />
            ))}
          </ul>
          {query.hasNextPage ? (
            <div className="flex justify-center">
              <Button
                variant="outline"
                size="sm"
                disabled={query.isFetchingNextPage}
                onClick={() => query.fetchNextPage()}
              >
                {query.isFetchingNextPage ? t('loading') : t('load-more')}
              </Button>
            </div>
          ) : null}
        </>
      )}
    </div>
  );
}
