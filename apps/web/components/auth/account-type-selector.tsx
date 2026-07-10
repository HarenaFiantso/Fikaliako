'use client';

import { useTranslations } from 'next-intl';

import { Store, UserRound } from 'lucide-react';
import { motion } from 'motion/react';

import { cn } from '@/lib/utils';

export type AccountType = 'consumer' | 'business';

const options = [
  { value: 'consumer', icon: UserRound, labelKey: 'consumer', hintKey: 'consumer-hint' },
  { value: 'business', icon: Store, labelKey: 'business', hintKey: 'business-hint' },
] as const;

export function AccountTypeSelector({
  value,
  onChange,
}: {
  value: AccountType;
  onChange: (value: AccountType) => void;
}) {
  const t = useTranslations('auth.fields.account-type');

  return (
    <div role="radiogroup" aria-label={t('label')} className="grid grid-cols-2 gap-3">
      {options.map((option) => {
        const selected = value === option.value;
        const Icon = option.icon;
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={selected}
            onClick={() => onChange(option.value)}
            className={cn(
              'relative rounded-xl border p-4 text-left transition-colors duration-200',
              selected
                ? 'border-primary/50 bg-primary/5'
                : 'hover:border-ring/40 hover:bg-accent/40'
            )}
          >
            {selected ? (
              <motion.span
                layoutId="account-type-ring"
                className="ring-primary/60 pointer-events-none absolute inset-0 rounded-xl ring-2"
                transition={{ type: 'spring', bounce: 0.25, duration: 0.5 }}
              />
            ) : null}
            <Icon className={cn('size-5', selected ? 'text-primary' : 'text-muted-foreground')} />
            <p className="mt-2.5 text-sm font-semibold">{t(option.labelKey)}</p>
            <p className="text-muted-foreground mt-0.5 text-xs leading-snug">{t(option.hintKey)}</p>
          </button>
        );
      })}
    </div>
  );
}
