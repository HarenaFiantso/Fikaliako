import { Star, StarHalf } from 'lucide-react';

import { cn } from '@/lib/utils';

export function StarRow({ value, className }: { value: number; className?: string }) {
  return (
    <span aria-hidden className={cn('text-primary flex items-center gap-0.5', className)}>
      {Array.from({ length: 5 }, (_, position) => {
        if (value >= position + 0.75) {
          return <Star key={position} className="size-3.5 fill-current" />;
        }
        if (value >= position + 0.25) {
          return <StarHalf key={position} className="size-3.5 fill-current" />;
        }
        return <Star key={position} className="size-3.5 opacity-40" />;
      })}
    </span>
  );
}
