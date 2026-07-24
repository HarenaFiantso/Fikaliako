'use client';

import { useTranslations } from 'next-intl';

import type { ContributionStatus } from '@fikaliako/api-client';

import { Badge } from '@/components/ui/badge';

import { cn } from '@/lib/utils';

const STATUS_STYLES: Record<ContributionStatus, string> = {
  pending: 'border-amber-600/30 bg-amber-500/10 text-amber-700 dark:text-amber-400',
  applied: 'border-emerald-600/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400',
  rejected: 'border-destructive/30 bg-destructive/10 text-destructive',
  superseded: 'border-border bg-muted text-muted-foreground',
};

/** Colour-coded moderation status of a contribution. */
export function StatusBadge({ status }: { status: ContributionStatus }) {
  const t = useTranslations('moderation.status');
  return (
    <Badge variant="outline" className={cn(STATUS_STYLES[status])}>
      {t(status)}
    </Badge>
  );
}
