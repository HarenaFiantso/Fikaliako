'use client';

import { useEffect, useState } from 'react';

import { useTranslations } from 'next-intl';

import { MapPin } from 'lucide-react';
import { AnimatePresence, motion } from 'motion/react';

import { BrandLogo } from '@/components/brand-logo';

const floatingDishes = [
  { label: 'Romazava', top: '18%', left: '12%', delay: '0s', duration: '7s' },
  { label: 'Mofo gasy', top: '30%', left: '68%', delay: '1.2s', duration: '8s' },
  { label: 'Ravitoto', top: '62%', left: '16%', delay: '0.6s', duration: '6.5s' },
  { label: 'Koba', top: '74%', left: '60%', delay: '1.8s', duration: '7.5s' },
  { label: 'Sambos', top: '48%', left: '78%', delay: '0.3s', duration: '9s' },
  { label: 'Lasary', top: '85%', left: '32%', delay: '2.4s', duration: '8.5s' },
];

const TAGLINE_INTERVAL_MS = 3600;

function RotatingTaglines({ taglines }: { taglines: string[] }) {
  const [index, setIndex] = useState(0);

  useEffect(() => {
    const id = setInterval(() => setIndex((i) => (i + 1) % taglines.length), TAGLINE_INTERVAL_MS);
    return () => clearInterval(id);
  }, [taglines.length]);

  return (
    <div className="h-16">
      <AnimatePresence mode="wait">
        <motion.p
          key={index}
          initial={{ opacity: 0, y: 16, filter: 'blur(8px)' }}
          animate={{ opacity: 1, y: 0, filter: 'blur(0px)' }}
          exit={{ opacity: 0, y: -16, filter: 'blur(8px)' }}
          transition={{ duration: 0.5, ease: 'easeOut' }}
          className="text-2xl font-semibold text-white/85 italic"
        >
          {taglines[index]}
        </motion.p>
      </AnimatePresence>
    </div>
  );
}

export function BrandPanel() {
  const t = useTranslations('auth.brand-panel');
  const taglines = t.raw('taglines') as string[];

  return (
    <aside className="relative hidden overflow-hidden bg-[linear-gradient(160deg,oklch(0.55_0.15_40)_0%,oklch(0.42_0.13_32)_55%,oklch(0.28_0.07_36)_100%)] lg:flex lg:w-[46%] lg:flex-col lg:justify-between lg:p-10 xl:p-14">
      <div className="animate-aurora absolute -top-1/4 -left-1/4 size-[70%] rounded-full bg-[radial-gradient(circle_at_center,oklch(0.78_0.14_65_/_0.5),transparent_70%)] blur-3xl" />
      <div
        className="animate-aurora absolute -right-1/4 -bottom-1/4 size-[80%] rounded-full bg-[radial-gradient(circle_at_center,oklch(0.6_0.16_25_/_0.45),transparent_70%)] blur-3xl"
        style={{ animationDelay: '-9s' }}
      />
      <div className="absolute inset-0 bg-[linear-gradient(to_right,rgba(255,255,255,0.07)_1px,transparent_1px),linear-gradient(to_bottom,rgba(255,255,255,0.07)_1px,transparent_1px)] [mask-image:radial-gradient(ellipse_at_center,black_35%,transparent_78%)] bg-[size:44px_44px]" />

      {floatingDishes.map((dish) => (
        <span
          key={dish.label}
          className="animate-float absolute rounded-full border border-white/15 bg-white/10 px-4 py-1.5 text-sm font-medium text-white/80 shadow-lg backdrop-blur-md"
          style={{
            top: dish.top,
            left: dish.left,
            animationDelay: dish.delay,
            animationDuration: dish.duration,
          }}
        >
          {dish.label}
        </span>
      ))}

      <motion.div
        initial={{ opacity: 0, y: -12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, ease: 'easeOut' }}
        className="relative"
      >
        <BrandLogo inverted />
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, delay: 0.15, ease: 'easeOut' }}
        className="relative max-w-md"
      >
        <h2 className="mb-4 text-4xl leading-tight font-extrabold text-white xl:text-5xl">
          {t('headline')}
        </h2>
        <RotatingTaglines taglines={taglines} />
      </motion.div>

      <motion.p
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.8, delay: 0.4 }}
        className="relative flex items-center gap-2 text-sm text-white/60"
      >
        <MapPin className="size-4" />
        {t('footnote')}
      </motion.p>
    </aside>
  );
}
