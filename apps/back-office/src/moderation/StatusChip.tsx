import type { ContributionStatus } from '@fikaliako/api-client';
import { Chip, type ChipProps } from '@mui/material';
import { useRecordContext } from 'react-admin';

import type { ContributionRecord } from './types';

const STATUS_PRESENTATION: Record<
  ContributionStatus,
  { label: string; color: ChipProps['color'] }
> = {
  pending: { label: 'Pending', color: 'warning' },
  applied: { label: 'Approved', color: 'success' },
  rejected: { label: 'Rejected', color: 'error' },
  superseded: { label: 'Superseded', color: 'default' },
};

/** The moderation status of a contribution, colour-coded. */
export function StatusChip() {
  const record = useRecordContext<ContributionRecord>();
  if (!record) return null;
  const { label, color } = STATUS_PRESENTATION[record.status];
  return <Chip label={label} color={color} size="small" variant="outlined" />;
}
