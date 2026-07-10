'use client';

import { motion, type Variants } from 'motion/react';

import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';

const containerVariants: Variants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.07, delayChildren: 0.08 } },
};

export const fieldVariants: Variants = {
  hidden: { opacity: 0, y: 12 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.35, ease: 'easeOut' } },
};

export function AuthCard({
  title,
  subtitle,
  children,
  footer,
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 24, scale: 0.98 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ duration: 0.45, ease: [0.22, 1, 0.36, 1] }}
    >
      <Card className="border-border/60 shadow-primary/5 gap-5 shadow-xl">
        <CardHeader>
          <CardTitle className="text-2xl">{title}</CardTitle>
          {subtitle ? (
            <CardDescription className="leading-relaxed">{subtitle}</CardDescription>
          ) : null}
        </CardHeader>
        <CardContent>
          <motion.div variants={containerVariants} initial="hidden" animate="visible">
            {children}
          </motion.div>
        </CardContent>
        {footer ? (
          <CardFooter className="justify-center border-t pt-5 [.border-t]:pt-5">
            {footer}
          </CardFooter>
        ) : null}
      </Card>
    </motion.div>
  );
}
