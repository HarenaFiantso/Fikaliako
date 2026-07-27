'use client';

import { useTranslations } from 'next-intl';
import { useRouter } from 'next/navigation';

import { Heart } from 'lucide-react';
import { motion } from 'motion/react';

import type { EstablishmentSummary } from '@fikaliako/api-client';

import { useFavoriteIds, useToggleFavorite } from '@/hooks/use-favorites';
import { useSession } from '@/hooks/use-session';

import { cn } from '@/lib/utils';

export function FavoriteButton({
  establishment,
  className,
}: {
  establishment: EstablishmentSummary;
  className?: string;
}) {
  const t = useTranslations('establishment.favorite');
  const router = useRouter();
  const { isAuthenticated } = useSession();
  const favoriteIds = useFavoriteIds();
  const toggle = useToggleFavorite();

  const favorite = favoriteIds.has(establishment.id);

  function onClick() {
    if (!isAuthenticated) {
      router.push('/login');
      return;
    }
    toggle.mutate({ establishment, favorite: !favorite });
  }

  return (
    <button
      type="button"
      aria-pressed={favorite}
      aria-label={favorite ? t('remove') : t('add')}
      title={favorite ? t('remove') : t('add')}
      onClick={onClick}
      className={cn(
        'text-muted-foreground hover:text-primary flex size-11 shrink-0 items-center justify-center rounded-full transition-colors',
        favorite && 'text-primary',
        className
      )}
    >
      <motion.span
        key={favorite ? 'on' : 'off'}
        initial={{ scale: favorite ? 1.35 : 0.75 }}
        animate={{ scale: 1 }}
        transition={{ type: 'spring', stiffness: 420, damping: 14 }}
        className="flex"
      >
        <Heart className={cn('size-6', favorite && 'fill-current')} />
      </motion.span>
    </button>
  );
}
