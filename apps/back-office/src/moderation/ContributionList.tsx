import { Box, Chip, Stack, Typography } from '@mui/material';
import {
  Datagrid,
  DateField,
  FunctionField,
  List,
  Pagination,
  SelectInput,
  TextField,
} from 'react-admin';

import { ApproveButton, RejectButton } from './actions';
import { StatusChip } from './StatusChip';
import { type ContributionRecord, formatAriary, proposalOf } from './types';

const STATUS_CHOICES = [
  { id: 'pending', name: 'Pending' },
  { id: 'applied', name: 'Approved' },
  { id: 'rejected', name: 'Rejected' },
  { id: 'superseded', name: 'Superseded' },
];

const filters = [
  <SelectInput key="status" source="status" label="Status" choices={STATUS_CHOICES} alwaysOn />,
];

const typeLabel = (type?: string) => (type ? type.replace(/_/g, ' ') : '—');

export function ContributionList() {
  return (
    <List
      title="Moderation queue"
      filters={filters}
      filterDefaultValues={{ status: 'pending' }}
      perPage={20}
      pagination={<Pagination rowsPerPageOptions={[10, 20, 50]} />}
      exporter={false}
      empty={false}
    >
      <Datagrid rowClick="show" bulkActionButtons={false}>
        <FunctionField
          label="Place"
          render={(record: ContributionRecord) => {
            const proposal = proposalOf(record);
            return (
              <Stack spacing={0.25}>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  {proposal.name ?? '—'}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {[proposal.district, proposal.city].filter(Boolean).join(' · ') || '—'}
                </Typography>
              </Stack>
            );
          }}
        />
        <FunctionField
          label="Type"
          render={(record: ContributionRecord) => (
            <Chip
              label={typeLabel(proposalOf(record).type)}
              size="small"
              variant="outlined"
              sx={{ textTransform: 'capitalize' }}
            />
          )}
        />
        <FunctionField
          label="Avg. price"
          render={(record: ContributionRecord) => {
            const price = proposalOf(record).avg_price_ar;
            return price == null ? '—' : formatAriary(price);
          }}
        />
        <TextField source="author_name" label="Author" emptyText="—" sortable={false} />
        <DateField source="created_at" label="Submitted" showTime sortable={false} />
        <FunctionField label="Status" render={() => <StatusChip />} />
        <FunctionField
          label={false}
          render={() => (
            <Box onClick={(event) => event.stopPropagation()}>
              <Stack direction="row" spacing={1}>
                <ApproveButton />
                <RejectButton />
              </Stack>
            </Box>
          )}
        />
      </Datagrid>
    </List>
  );
}
