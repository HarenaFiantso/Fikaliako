'use client';

import { useTranslations } from 'next-intl';

import type { ReviewPage } from '@fikaliako/api-client';

import { FormAlert } from '@/components/auth/form-alert';

import { Button } from '@/components/ui/button';

import { ReviewCard } from '@/components/establishments/review-card';

import { useEstablishmentReviews } from '@/hooks/use-establishments';

export function EstablishmentReviews({
  establishmentId,
  initialPage,
}: {
  establishmentId: string;
  initialPage?: ReviewPage;
}) {
  const t = useTranslations('establishment.reviews');
  const query = useEstablishmentReviews(establishmentId, initialPage);
  const items = query.data?.pages.flatMap((page) => page.items) ?? [];

  if (query.isPending) {
    return (
      <ul className="space-y-3" aria-busy>
        {Array.from({ length: 2 }, (_, index) => (
          <li key={index} className="bg-muted h-24 animate-pulse rounded-2xl" />
        ))}
      </ul>
    );
  }

  if (query.isError && items.length === 0) {
    return (
      <div className="space-y-3">
        <FormAlert message={t('error')} />
        <Button variant="outline" size="sm" onClick={() => query.refetch()}>
          {t('retry')}
        </Button>
      </div>
    );
  }

  if (items.length === 0) {
    return <p className="text-muted-foreground text-sm">{t('empty')}</p>;
  }

  return (
    <div className="space-y-3">
      <ul className="space-y-3">
        {items.map((review) => (
          <ReviewCard key={review.id} review={review} />
        ))}
      </ul>
      {query.hasNextPage ? (
        <Button
          variant="ghost"
          size="sm"
          disabled={query.isFetchingNextPage}
          onClick={() => query.fetchNextPage()}
        >
          {query.isFetchingNextPage ? t('loading') : t('load-more')}
        </Button>
      ) : null}
    </div>
  );
}
