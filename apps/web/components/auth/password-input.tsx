'use client';

import { useState } from 'react';

import { useTranslations } from 'next-intl';

import { Eye, EyeOff } from 'lucide-react';

import { Input } from '@/components/ui/input';

import { cn } from '@/lib/utils';

export function PasswordInput({ className, ...props }: React.ComponentProps<'input'>) {
  const [visible, setVisible] = useState(false);
  const t = useTranslations('auth.fields.password');

  return (
    <div className="relative">
      <Input type={visible ? 'text' : 'password'} className={cn('pr-11', className)} {...props} />
      <button
        type="button"
        tabIndex={-1}
        onClick={() => setVisible((v) => !v)}
        aria-label={visible ? t('hide') : t('show')}
        className="text-muted-foreground hover:text-foreground absolute inset-y-0 right-0 flex w-11 items-center justify-center transition-colors"
      >
        {visible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
      </button>
    </div>
  );
}
