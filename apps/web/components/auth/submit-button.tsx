'use client';

import { LoaderCircle } from 'lucide-react';

import { Button } from '@/components/ui/button';

import { cn } from '@/lib/utils';

export function SubmitButton({
  pending = false,
  className,
  children,
  ...props
}: React.ComponentProps<typeof Button> & { pending?: boolean }) {
  return (
    <Button
      type="submit"
      size="lg"
      disabled={pending}
      className={cn('w-full', className)}
      {...props}
    >
      {pending ? <LoaderCircle className="animate-spin" /> : null}
      {children}
    </Button>
  );
}
