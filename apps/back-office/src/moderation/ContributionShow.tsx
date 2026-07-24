import type { ReactNode } from 'react';

import { Box, Card, CardContent, Chip, Link, Stack, Typography } from '@mui/material';
import { Show, TopToolbar, useRecordContext } from 'react-admin';

import { ApproveButton, RejectButton } from './actions';
import { StatusChip } from './StatusChip';
import { type ContributionRecord, formatAriary, proposalOf } from './types';

function Detail({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Box>
      <Typography
        variant="caption"
        color="text.secondary"
        sx={{ textTransform: 'uppercase', letterSpacing: 0.6, fontWeight: 600 }}
      >
        {label}
      </Typography>
      <Typography variant="body2" component="div" sx={{ mt: 0.25 }}>
        {children}
      </Typography>
    </Box>
  );
}

const formatDateTime = (value?: string) => (value ? new Date(value).toLocaleString() : '—');

function ContributionTitle() {
  const record = useRecordContext<ContributionRecord>();
  return <span>{record ? (proposalOf(record).name ?? 'Contribution') : 'Contribution'}</span>;
}

function ContributionShowActions() {
  return (
    <TopToolbar>
      <ApproveButton />
      <RejectButton />
    </TopToolbar>
  );
}

function ContributionDetails() {
  const record = useRecordContext<ContributionRecord>();
  if (!record) return null;
  const proposal = proposalOf(record);

  return (
    <Stack spacing={2} sx={{ p: 2 }}>
      <Card variant="outlined">
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Proposed place
          </Typography>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
              gap: 2,
            }}
          >
            <Detail label="Name">{proposal.name ?? '—'}</Detail>
            <Detail label="Type">
              <Chip
                label={proposal.type?.replace(/_/g, ' ') ?? '—'}
                size="small"
                variant="outlined"
                sx={{ textTransform: 'capitalize' }}
              />
            </Detail>
            <Detail label="Average price">
              {proposal.avg_price_ar == null ? '—' : formatAriary(proposal.avg_price_ar)}
            </Detail>
            <Detail label="Address">{proposal.address ?? '—'}</Detail>
            <Detail label="District">{proposal.district ?? '—'}</Detail>
            <Detail label="City">{proposal.city ?? '—'}</Detail>
            <Detail label="Phone">{proposal.phone ?? '—'}</Detail>
            <Detail label="Position">
              {proposal.lat != null && proposal.lng != null ? (
                <Link
                  href={`https://www.openstreetmap.org/?mlat=${proposal.lat}&mlon=${proposal.lng}#map=18/${proposal.lat}/${proposal.lng}`}
                  target="_blank"
                  rel="noreferrer"
                >
                  {proposal.lat.toFixed(5)}, {proposal.lng.toFixed(5)}
                </Link>
              ) : (
                '—'
              )}
            </Detail>
          </Box>
          {proposal.comment ? (
            <Box sx={{ mt: 2 }}>
              <Detail label="Note for the moderators">
                <Typography variant="body2" sx={{ fontStyle: 'italic' }}>
                  “{proposal.comment}”
                </Typography>
              </Detail>
            </Box>
          ) : null}
        </CardContent>
      </Card>

      <Card variant="outlined">
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Moderation
          </Typography>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
              gap: 2,
            }}
          >
            <Detail label="Status">
              <StatusChip />
            </Detail>
            <Detail label="Author">{record.author_name ?? '—'}</Detail>
            <Detail label="Submitted">{formatDateTime(record.created_at)}</Detail>
            <Detail label="Confirmations">{record.confirmations_count}</Detail>
            <Detail label="Resolved">{formatDateTime(record.resolved_at)}</Detail>
            {record.establishment_id ? (
              <Detail label="Published establishment">
                <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                  {record.establishment_id}
                </Typography>
              </Detail>
            ) : null}
          </Box>
          {record.resolution_note ? (
            <Box sx={{ mt: 2 }}>
              <Detail label="Resolution note">{record.resolution_note}</Detail>
            </Box>
          ) : null}
        </CardContent>
      </Card>
    </Stack>
  );
}

export function ContributionShow() {
  return (
    <Show title={<ContributionTitle />} actions={<ContributionShowActions />}>
      <ContributionDetails />
    </Show>
  );
}
