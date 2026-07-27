'use client';

import { useTranslations } from 'next-intl';
import Link from 'next/link';

import { BadgeCheck, Star } from 'lucide-react';

import type { EstablishmentSummary } from '@fikaliako/api-client';

import { FavoriteButton } from '@/components/establishments/favorite-button';

import { TYPE_ICONS } from '@/lib/establishments';
import { formatAriary } from '@/lib/format';

export function EstablishmentCard({ establishment }: { establishment: EstablishmentSummary }) {
  const t = useTranslations('establishment');
  const TypeIcon = TYPE_ICONS[establishment.type];

  const rated = establishment.rating_avg != null && establishment.rating_count > 0;

  return (
    <li className="bg-card hover:bg-accent/40 focus-within:ring-ring/50 relative flex items-center gap-3 rounded-2xl border p-3 transition-colors focus-within:ring-2">
      <div className="bg-accent text-primary flex size-12 shrink-0 items-center justify-center rounded-xl">
        <TypeIcon className="size-5.5" />
      </div>

      <div className="min-w-0 flex-1 space-y-0.5">
        <div className="flex items-center gap-1.5">
          <Link
            href={`/establishment/${establishment.slug}`}
            className="truncate font-bold after:absolute after:inset-0 after:rounded-2xl hover:underline focus-visible:outline-none"
          >
            {establishment.name}
          </Link>
          {establishment.verified ? (
            <BadgeCheck
              aria-label={t('verified')}
              className="size-4 shrink-0 text-emerald-600 dark:text-emerald-400"
            />
          ) : null}
        </div>

        <p className="text-muted-foreground truncate text-sm">
          {t(`types.${establishment.type}`)}
          {establishment.avg_price_ar != null
            ? ` · ~${formatAriary(establishment.avg_price_ar)}`
            : ''}
        </p>

        {rated ? (
          <p className="flex items-center gap-1 text-sm">
            <Star className="text-primary size-3.5 fill-current" />
            <span className="font-semibold">{establishment.rating_avg?.toFixed(1)}</span>
            <span className="text-muted-foreground">({establishment.rating_count})</span>
          </p>
        ) : (
          <p className="text-muted-foreground text-sm">{t('no-reviews')}</p>
        )}
      </div>

      <FavoriteButton establishment={establishment} className="relative" />
    </li>
  );
}
