'use client';

import { useState } from 'react';

import { useFormatter, useTranslations } from 'next-intl';
import Link from 'next/link';

import { Inbox } from 'lucide-react';

import type { ContributionItem, ContributionStatus } from '@fikaliako/api-client';

import { FormAlert } from '@/components/auth/form-alert';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';

import { ContributionActions } from '@/components/moderation/contribution-actions';
import { StatusBadge } from '@/components/moderation/status-badge';

import { useModerationQueue } from '@/hooks/use-moderation';

import { formatAriary, humanizeType } from '@/lib/format';
import { proposalOf } from '@/lib/moderation';
import { cn } from '@/lib/utils';

const STATUSES: ContributionStatus[] = ['pending', 'applied', 'rejected', 'superseded'];

function QueueRow({ contribution }: { contribution: ContributionItem }) {
  const t = useTranslations('moderation.queue');
  const format = useFormatter();
  const proposal = proposalOf(contribution);
  const area = [proposal.district, proposal.city].filter(Boolean).join(' · ');

  return (
    <li className="flex flex-col gap-3 px-6 py-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="min-w-0 space-y-1">
        <div className="flex flex-wrap items-center gap-2">
          <Link
            href={`/moderation/${contribution.id}`}
            className="hover:text-primary truncate font-semibold hover:underline"
          >
            {proposal.name ?? '—'}
          </Link>
          <Badge variant="outline" className="capitalize">
            {humanizeType(proposal.type)}
          </Badge>
          {proposal.avg_price_ar != null ? (
            <span className="text-muted-foreground text-xs">
              {formatAriary(proposal.avg_price_ar)}
            </span>
          ) : null}
        </div>
        <p className="text-muted-foreground text-xs">
          {area || '—'}
          {' · '}
          {t('submitted-by', { name: contribution.author_name ?? t('anonymous') })}
          {' · '}
          {format.dateTime(new Date(contribution.created_at), {
            dateStyle: 'medium',
            timeStyle: 'short',
          })}
        </p>
      </div>
      <div className="flex shrink-0 items-center gap-3">
        <StatusBadge status={contribution.status} />
        <ContributionActions contribution={contribution} />
      </div>
    </li>
  );
}

export function ModerationQueue() {
  const t = useTranslations('moderation');
  const [status, setStatus] = useState<ContributionStatus>('pending');
  const query = useModerationQueue(status);
  const items = query.data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-extrabold tracking-tight">{t('title')}</h1>
        <p className="text-muted-foreground text-sm">{t('subtitle')}</p>
      </div>

      <div className="flex flex-wrap gap-2" role="tablist" aria-label={t('title')}>
        {STATUSES.map((value) => (
          <button
            key={value}
            type="button"
            role="tab"
            aria-selected={status === value}
            onClick={() => setStatus(value)}
            className={cn(
              'rounded-full border px-3.5 py-1.5 text-sm font-semibold transition-colors',
              status === value
                ? 'border-primary/25 bg-primary/10 text-primary'
                : 'border-border text-muted-foreground hover:bg-accent hover:text-accent-foreground'
            )}
          >
            {t(`status.${value}`)}
          </button>
        ))}
      </div>

      {query.isPending ? (
        <div className="space-y-3" aria-busy>
          {Array.from({ length: 3 }, (_, index) => (
            <div key={index} className="bg-muted h-20 animate-pulse rounded-2xl" />
          ))}
        </div>
      ) : query.isError ? (
        <div>
          <FormAlert message={t('queue.error')} />
          <Button variant="outline" size="sm" onClick={() => query.refetch()}>
            {t('queue.retry')}
          </Button>
        </div>
      ) : items.length === 0 ? (
        <Card>
          <CardContent className="text-muted-foreground flex flex-col items-center gap-3 py-10 text-center text-sm">
            <Inbox className="size-8 opacity-60" />
            {t('queue.empty')}
          </CardContent>
        </Card>
      ) : (
        <>
          <Card className="py-0">
            <ul className="divide-border/60 divide-y">
              {items.map((contribution) => (
                <QueueRow key={contribution.id} contribution={contribution} />
              ))}
            </ul>
          </Card>
          {query.hasNextPage ? (
            <div className="flex justify-center">
              <Button
                variant="outline"
                size="sm"
                disabled={query.isFetchingNextPage}
                onClick={() => query.fetchNextPage()}
              >
                {query.isFetchingNextPage ? t('queue.loading') : t('queue.load-more')}
              </Button>
            </div>
          ) : null}
        </>
      )}
    </div>
  );
}
