import Link from 'next/link';

import { UtensilsCrossed } from 'lucide-react';

import { cn } from '@/lib/utils';

export function BrandLogo({
  className,
  inverted = false,
}: {
  className?: string;
  inverted?: boolean;
}) {
  return (
    <Link
      href="/"
      className={cn('group flex items-center gap-2.5 font-bold tracking-tight', className)}
    >
      <span
        className={cn(
          'flex size-9 items-center justify-center rounded-xl shadow-sm transition-transform duration-300 group-hover:-rotate-6',
          inverted ? 'bg-white/15 text-white backdrop-blur' : 'bg-primary text-primary-foreground'
        )}
      >
        <UtensilsCrossed className="size-4.5" />
      </span>
      <span className={cn('text-lg', inverted && 'text-white')}>Fikaliako</span>
    </Link>
  );
}
