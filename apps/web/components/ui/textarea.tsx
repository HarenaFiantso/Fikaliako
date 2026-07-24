import * as React from 'react';

import { cn } from '@/lib/utils';

function Textarea({ className, ...props }: React.ComponentProps<'textarea'>) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        'border-input flex min-h-20 w-full rounded-lg border bg-transparent px-3.5 py-2.5 text-sm shadow-xs transition-[color,box-shadow] outline-none',
        'placeholder:text-muted-foreground selection:bg-primary/20 disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50',
        'focus-visible:border-ring focus-visible:ring-ring/30 focus-visible:ring-[3px]',
        'aria-invalid:border-destructive aria-invalid:ring-destructive/20',
        className
      )}
      {...props}
    />
  );
}

export { Textarea };
