import { getTranslations } from 'next-intl/server';

import type { OpeningInterval } from '@fikaliako/api-client';

import { antananarivoDayIndex, intervalsOfDay, WEEKDAY_KEYS } from '@/lib/establishments';
import { cn } from '@/lib/utils';

export async function OpeningHours({ intervals }: { intervals: OpeningInterval[] }) {
  const t = await getTranslations('establishment');
  const today = antananarivoDayIndex();

  return (
    <dl className="space-y-2 text-sm">
      {WEEKDAY_KEYS.map((day, index) => {
        const slots = intervalsOfDay(intervals, index)
          .map((interval) => `${interval.opens_at} – ${interval.closes_at}`)
          .join(', ');
        const isToday = index === today;

        return (
          <div key={day} className="flex items-center justify-between gap-3">
            <dt className={cn(isToday ? 'font-semibold' : 'text-muted-foreground')}>
              {t(`weekdays.${day}`)}
            </dt>
            <dd className={cn(isToday && slots ? 'font-semibold' : 'text-muted-foreground')}>
              {slots || t('closed')}
            </dd>
          </div>
        );
      })}
    </dl>
  );
}
