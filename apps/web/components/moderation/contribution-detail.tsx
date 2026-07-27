'use client';

import { useFormatter, useTranslations } from 'next-intl';
import Link from 'next/link';

import { ArrowLeft, ExternalLink } from 'lucide-react';

import { FormAlert } from '@/components/auth/form-alert';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

import { ContributionActions } from '@/components/moderation/contribution-actions';
import { StatusBadge } from '@/components/moderation/status-badge';

import { useContribution } from '@/hooks/use-moderation';

import { formatAriary, humanizeType } from '@/lib/format';
import { proposalOf } from '@/lib/moderation';

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <p className="text-muted-foreground text-xs font-semibold tracking-wide uppercase">{label}</p>
      <div className="mt-0.5 text-sm">{children}</div>
    </div>
  );
}

export function ContributionDetail({ contributionId }: { contributionId: string }) {
  const t = useTranslations('moderation.detail');
  const format = useFormatter();
  const query = useContribution(contributionId);

  const dateTime = (value?: string) =>
    value ? format.dateTime(new Date(value), { dateStyle: 'medium', timeStyle: 'short' }) : '—';

  if (query.isPending) {
    return (
      <div className="space-y-3" aria-busy>
        <div className="bg-muted h-9 w-64 animate-pulse rounded-lg" />
        <div className="bg-muted h-56 animate-pulse rounded-2xl" />
        <div className="bg-muted h-40 animate-pulse rounded-2xl" />
      </div>
    );
  }

  if (query.isError) {
    return (
      <div>
        <FormAlert message={t('error')} />
        <Button variant="outline" size="sm" asChild>
          <Link href="/moderation">
            <ArrowLeft />
            {t('back')}
          </Link>
        </Button>
      </div>
    );
  }

  const contribution = query.data;
  const proposal = proposalOf(contribution);

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" className="-ml-3" asChild>
        <Link href="/moderation">
          <ArrowLeft />
          {t('back')}
        </Link>
      </Button>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-extrabold tracking-tight">{proposal.name ?? '—'}</h1>
          <StatusBadge status={contribution.status} />
        </div>
        <ContributionActions contribution={contribution} />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{t('proposed-place')}</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Field label={t('name')}>{proposal.name ?? '—'}</Field>
          <Field label={t('type')}>
            <Badge variant="outline" className="capitalize">
              {humanizeType(proposal.type)}
            </Badge>
          </Field>
          <Field label={t('avg-price')}>
            {proposal.avg_price_ar == null ? '—' : formatAriary(proposal.avg_price_ar)}
          </Field>
          <Field label={t('address')}>{proposal.address ?? '—'}</Field>
          <Field label={t('district')}>{proposal.district ?? '—'}</Field>
          <Field label={t('city')}>{proposal.city ?? '—'}</Field>
          <Field label={t('phone')}>{proposal.phone ?? '—'}</Field>
          <Field label={t('position')}>
            {proposal.lat != null && proposal.lng != null ? (
              <a
                href={`https://www.openstreetmap.org/?mlat=${proposal.lat}&mlon=${proposal.lng}#map=18/${proposal.lat}/${proposal.lng}`}
                target="_blank"
                rel="noreferrer"
                className="text-primary inline-flex items-center gap-1 font-medium hover:underline"
              >
                {proposal.lat.toFixed(5)}, {proposal.lng.toFixed(5)}
                <ExternalLink className="size-3.5" />
              </a>
            ) : (
              '—'
            )}
          </Field>
          {proposal.comment ? (
            <div className="sm:col-span-2 lg:col-span-3">
              <Field label={t('comment')}>
                <p className="italic">« {proposal.comment} »</p>
              </Field>
            </div>
          ) : null}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{t('moderation')}</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Field label={t('status')}>
            <StatusBadge status={contribution.status} />
          </Field>
          <Field label={t('author')}>{contribution.author_name ?? '—'}</Field>
          <Field label={t('submitted')}>{dateTime(contribution.created_at)}</Field>
          <Field label={t('confirmations')}>{contribution.confirmations_count}</Field>
          <Field label={t('resolved')}>{dateTime(contribution.resolved_at)}</Field>
          {contribution.establishment_id ? (
            <Field label={t('establishment')}>
              <span className="font-mono text-xs">{contribution.establishment_id}</span>
            </Field>
          ) : null}
          {contribution.resolution_note ? (
            <div className="sm:col-span-2 lg:col-span-3">
              <Field label={t('resolution-note')}>{contribution.resolution_note}</Field>
            </div>
          ) : null}
        </CardContent>
      </Card>
    </div>
  );
}
