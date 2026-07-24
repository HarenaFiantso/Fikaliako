'use client';

import { useState } from 'react';

import { useTranslations } from 'next-intl';

import { CircleCheck, CircleX } from 'lucide-react';
import { toast } from 'sonner';

import type { ContributionItem } from '@fikaliako/api-client';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';

import { useApproveContribution, useRejectContribution } from '@/hooks/use-moderation';

import { ApiError } from '@/lib/api/api-error';
import { proposalOf } from '@/lib/moderation';

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiError && error.problem?.detail ? error.problem.detail : fallback;
}

/**
 * Approve / reject-with-reason for a pending contribution. Renders nothing
 * once the contribution is resolved.
 */
export function ContributionActions({ contribution }: { contribution: ContributionItem }) {
  const t = useTranslations('moderation.actions');
  const approve = useApproveContribution();
  const reject = useRejectContribution();
  const [dialog, setDialog] = useState<'approve' | 'reject' | null>(null);
  const [reason, setReason] = useState('');

  if (contribution.status !== 'pending') return null;
  const name = proposalOf(contribution).name ?? '—';
  const busy = approve.isPending || reject.isPending;

  const close = () => {
    setDialog(null);
    setReason('');
  };

  function handleApprove() {
    approve.mutate(contribution.id, {
      onSuccess: () => {
        close();
        toast.success(t('approve-success'));
      },
      onError: (error) => toast.error(errorMessage(error, t('error'))),
    });
  }

  function handleReject() {
    reject.mutate(
      { contributionId: contribution.id, reason },
      {
        onSuccess: () => {
          close();
          toast.success(t('reject-success'));
        },
        onError: (error) => toast.error(errorMessage(error, t('error'))),
      }
    );
  }

  return (
    <div className="flex items-center gap-2">
      <Button size="sm" disabled={busy} onClick={() => setDialog('approve')}>
        <CircleCheck />
        {t('approve')}
      </Button>
      <Button
        size="sm"
        variant="outline"
        className="text-destructive border-destructive/40 hover:bg-destructive/10 hover:text-destructive"
        disabled={busy}
        onClick={() => setDialog('reject')}
      >
        <CircleX />
        {t('reject')}
      </Button>

      <Dialog open={dialog === 'approve'} onOpenChange={(open) => !open && close()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('approve-title')}</DialogTitle>
            <DialogDescription>{t('approve-body', { name })}</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="ghost" size="sm" disabled={approve.isPending}>
                {t('cancel')}
              </Button>
            </DialogClose>
            <Button size="sm" disabled={approve.isPending} onClick={handleApprove}>
              {approve.isPending ? t('working') : t('approve-confirm')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={dialog === 'reject'} onOpenChange={(open) => !open && close()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('reject-title')}</DialogTitle>
            <DialogDescription>{t('reject-body', { name })}</DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="reject-reason">{t('reject-reason')}</Label>
            <Textarea
              id="reject-reason"
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              maxLength={500}
              autoFocus
            />
          </div>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="ghost" size="sm" disabled={reject.isPending}>
                {t('cancel')}
              </Button>
            </DialogClose>
            <Button
              size="sm"
              variant="destructive"
              disabled={reject.isPending}
              onClick={handleReject}
            >
              {reject.isPending ? t('working') : t('reject-confirm')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
