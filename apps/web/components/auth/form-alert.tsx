'use client';

import { CircleAlert, Info } from 'lucide-react';
import { AnimatePresence, motion } from 'motion/react';

import { cn } from '@/lib/utils';

export function FormAlert({
  message,
  variant = 'destructive',
}: {
  message: string | null;
  variant?: 'destructive' | 'info';
}) {
  return (
    <AnimatePresence initial={false}>
      {message ? (
        <motion.div
          initial={{ opacity: 0, height: 0, marginBottom: 0 }}
          animate={{ opacity: 1, height: 'auto', marginBottom: 16 }}
          exit={{ opacity: 0, height: 0, marginBottom: 0 }}
          transition={{ duration: 0.25, ease: 'easeOut' }}
          className="overflow-hidden"
        >
          <div
            role="alert"
            className={cn(
              'flex items-start gap-2.5 rounded-lg border px-3.5 py-3 text-sm font-medium',
              variant === 'destructive'
                ? 'border-destructive/30 bg-destructive/10 text-destructive'
                : 'border-primary/30 bg-primary/10 text-primary'
            )}
          >
            {variant === 'destructive' ? (
              <CircleAlert className="mt-0.5 size-4 shrink-0" />
            ) : (
              <Info className="mt-0.5 size-4 shrink-0" />
            )}
            {message}
          </div>
        </motion.div>
      ) : null}
    </AnimatePresence>
  );
}
