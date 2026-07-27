'use client';

import { useFormatter } from 'next-intl';

import type { ReviewItem } from '@fikaliako/api-client';

import { StarRow } from '@/components/establishments/star-row';

export function ReviewCard({ review }: { review: ReviewItem }) {
  const format = useFormatter();
  const initial = review.author_name.trim()[0]?.toUpperCase() ?? '?';

  return (
    <li className="bg-card space-y-2 rounded-2xl border p-4">
      <div className="flex items-center gap-3">
        <span className="bg-accent text-primary flex size-9 shrink-0 items-center justify-center rounded-full text-sm font-semibold">
          {initial}
        </span>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold">{review.author_name}</p>
          <p className="text-muted-foreground text-xs">
            {format.dateTime(new Date(review.created_at), { dateStyle: 'medium' })}
          </p>
        </div>
        <div className="flex flex-col items-end gap-0.5">
          <StarRow value={review.global_note} />
          <span className="text-sm font-semibold">{review.global_note.toFixed(1)}</span>
        </div>
      </div>
      {review.comment ? <p className="text-sm leading-relaxed">{review.comment}</p> : null}
    </li>
  );
}
