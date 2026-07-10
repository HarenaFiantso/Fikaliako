'use client';

import { useTranslations } from 'next-intl';
import Link from 'next/link';

import { ArrowRight, MapPin, Sparkles } from 'lucide-react';
import { motion } from 'motion/react';

import { Button } from '@/components/ui/button';

import { useSession } from '@/hooks/use-session';

export function LandingHero() {
  const t = useTranslations('landing');
  const { user, isAuthenticated } = useSession();

  return (
    <section className="relative overflow-hidden">
      <div className="animate-aurora bg-primary/15 absolute -top-32 left-1/4 size-96 rounded-full blur-3xl" />
      <div
        className="animate-aurora bg-accent absolute top-24 -right-16 size-80 rounded-full blur-3xl"
        style={{ animationDelay: '-8s' }}
      />
      <div className="absolute inset-0 bg-[linear-gradient(to_right,var(--border)_1px,transparent_1px),linear-gradient(to_bottom,var(--border)_1px,transparent_1px)] [mask-image:radial-gradient(ellipse_at_top,black_20%,transparent_70%)] bg-[size:48px_48px] opacity-40" />

      <div className="relative mx-auto flex max-w-4xl flex-col items-center px-4 pt-24 pb-28 text-center sm:px-6">
        <motion.p
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="border-primary/25 bg-primary/10 text-primary mb-6 flex items-center gap-1.5 rounded-full border px-4 py-1.5 text-sm font-semibold"
        >
          <MapPin className="size-3.5" />
          Antananarivo
        </motion.p>

        <motion.h1
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.08 }}
          className="from-foreground via-foreground to-primary max-w-2xl bg-gradient-to-br bg-clip-text text-4xl leading-tight font-extrabold tracking-tight text-transparent sm:text-5xl md:text-6xl"
        >
          {t('hero-title')}
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.16 }}
          className="text-muted-foreground mt-6 max-w-xl text-lg leading-relaxed"
        >
          {t('hero-subtitle')}
        </motion.p>

        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.24 }}
          className="mt-10"
        >
          {isAuthenticated && user ? (
            <p className="border-border bg-card flex items-center gap-2 rounded-full border px-5 py-2.5 text-sm font-medium shadow-sm">
              <Sparkles className="text-primary size-4" />
              {t('welcome', { name: user.display_name })}
            </p>
          ) : (
            <div className="flex flex-col items-center gap-3 sm:flex-row">
              <Button size="lg" asChild>
                <Link href="/register">
                  {t('cta-register')}
                  <ArrowRight />
                </Link>
              </Button>
              <Button size="lg" variant="outline" asChild>
                <Link href="/login">{t('cta-explore')}</Link>
              </Button>
            </div>
          )}
        </motion.div>
      </div>
    </section>
  );
}
