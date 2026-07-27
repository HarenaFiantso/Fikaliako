'use client';

import { useTranslations } from 'next-intl';

import { motion } from 'motion/react';

import type { RatingSummary } from '@fikaliako/api-client';

import { StarRow } from '@/components/establishments/star-row';

import { REVIEW_CRITERIA } from '@/lib/establishments';

export function RatingBreakdown({ rating }: { rating: RatingSummary }) {
  const t = useTranslations('establishment');

  if (rating.count === 0 || rating.avg_global == null) return null;

  return (
    <div className="bg-card flex items-center gap-6 rounded-2xl border p-4">
      <div className="flex flex-col items-center gap-1">
        <p className="text-4xl leading-none font-extrabold">{rating.avg_global.toFixed(1)}</p>
        <StarRow value={rating.avg_global} />
        <p className="text-muted-foreground text-sm">
          {t('review-count', { count: rating.count })}
        </p>
      </div>

      <dl className="flex-1 space-y-2">
        {REVIEW_CRITERIA.map((criterion, index) => {
          const value = rating[criterion];
          if (value == null) return null;

          return (
            <div key={criterion} className="flex items-center gap-2 text-sm">
              <dt className="text-muted-foreground w-20 shrink-0">{t(`criteria.${criterion}`)}</dt>
              <dd className="flex flex-1 items-center gap-2">
                <span className="bg-muted h-1.5 flex-1 overflow-hidden rounded-full">
                  <motion.span
                    className="bg-primary block h-full rounded-full"
                    initial={{ width: 0 }}
                    animate={{ width: `${(value / 5) * 100}%` }}
                    transition={{ duration: 0.6, delay: index * 0.08 }}
                  />
                </span>
                <span className="w-7 text-right font-semibold">{value.toFixed(1)}</span>
              </dd>
            </div>
          );
        })}
      </dl>
    </div>
  );
}
