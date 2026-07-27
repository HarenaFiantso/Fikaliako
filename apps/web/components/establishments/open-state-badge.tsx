import { getTranslations } from 'next-intl/server';

import type { OpenState } from '@/lib/establishments';
import { cn } from '@/lib/utils';

const STATE_STYLES: Record<OpenState, string> = {
  open: 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400',
  'closed-now': 'bg-destructive/10 text-destructive',
  closed: 'bg-destructive/10 text-destructive',
  pending: 'bg-muted text-muted-foreground',
};

export async function OpenStateBadge({ state }: { state: OpenState }) {
  const t = await getTranslations('establishment.open-state');

  return (
    <span
      className={cn(
        'flex items-center gap-1.5 rounded-full px-3 py-1 text-sm font-semibold',
        STATE_STYLES[state]
      )}
    >
      <span className="size-2 rounded-full bg-current" />
      {t(state)}
    </span>
  );
}
