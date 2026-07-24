import CancelRoundedIcon from '@mui/icons-material/CancelRounded';
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  TextField,
} from '@mui/material';
import { useState } from 'react';
import { Confirm, useDataProvider, useNotify, useRecordContext, useRefresh } from 'react-admin';

import type { ModerationDataProvider } from './data-provider';
import { proposalOf, type ContributionRecord } from './types';

function useModerationAction() {
  const dataProvider = useDataProvider<ModerationDataProvider>();
  const notify = useNotify();
  const refresh = useRefresh();
  const [busy, setBusy] = useState(false);

  async function run(action: () => Promise<unknown>, successMessage: string) {
    setBusy(true);
    try {
      await action();
      notify(successMessage, { type: 'success' });
      refresh();
    } catch (error) {
      notify(error instanceof Error ? error.message : 'The action failed.', { type: 'error' });
    } finally {
      setBusy(false);
    }
  }

  return { dataProvider, busy, run };
}

/** Approves a pending contribution after confirmation — the place goes live. */
export function ApproveButton() {
  const record = useRecordContext<ContributionRecord>();
  const { dataProvider, busy, run } = useModerationAction();
  const [open, setOpen] = useState(false);
  if (!record || record.status !== 'pending') return null;
  const name = proposalOf(record).name ?? 'this proposal';

  return (
    <>
      <Button
        size="small"
        variant="contained"
        color="success"
        startIcon={<CheckCircleRoundedIcon />}
        disabled={busy}
        onClick={() => setOpen(true)}
      >
        Approve
      </Button>
      <Confirm
        isOpen={open}
        loading={busy}
        title="Approve this place?"
        content={`“${name}” will be published and immediately visible to everyone.`}
        onConfirm={() => {
          setOpen(false);
          void run(
            () => dataProvider.approve(String(record.id)),
            'Contribution approved — the place is now public.',
          );
        }}
        onClose={() => setOpen(false)}
      />
    </>
  );
}

/** Rejects a pending contribution, with an optional reason kept as the note. */
export function RejectButton() {
  const record = useRecordContext<ContributionRecord>();
  const { dataProvider, busy, run } = useModerationAction();
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState('');
  if (!record || record.status !== 'pending') return null;
  const name = proposalOf(record).name ?? 'this proposal';

  const close = () => {
    setOpen(false);
    setReason('');
  };

  return (
    <>
      <Button
        size="small"
        variant="outlined"
        color="error"
        startIcon={<CancelRoundedIcon />}
        disabled={busy}
        onClick={() => setOpen(true)}
      >
        Reject
      </Button>
      <Dialog open={open} onClose={close} fullWidth maxWidth="xs">
        <DialogTitle>Reject this place?</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            “{name}” will stay unpublished. The reason is kept with the decision.
          </DialogContentText>
          <TextField
            label="Reason (optional)"
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            multiline
            minRows={2}
            fullWidth
            autoFocus
            slotProps={{ htmlInput: { maxLength: 500 } }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={close}>Cancel</Button>
          <Button
            variant="contained"
            color="error"
            disabled={busy}
            onClick={() => {
              const note = reason;
              close();
              void run(
                () => dataProvider.reject(String(record.id), note),
                'Contribution rejected.',
              );
            }}
          >
            Reject
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
