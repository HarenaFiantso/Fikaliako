'use client';

import { useMemo, useState } from 'react';

import { useTranslations } from 'next-intl';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft } from 'lucide-react';
import { motion } from 'motion/react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';

import { AuthCard, fieldVariants } from '@/components/auth/auth-card';
import { FormAlert } from '@/components/auth/form-alert';
import { SubmitButton } from '@/components/auth/submit-button';

import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';

import { useForgotPassword } from '@/hooks/use-auth-mutations';

import { toAuthErrorKey } from '@/lib/api/api-error';
import { usePendingAuthStore } from '@/lib/auth/pending-auth-store';
import {
  createForgotPasswordSchema,
  type ForgotPasswordValues,
} from '@/lib/validation/auth-schemas';

export function ForgotPasswordForm() {
  const t = useTranslations('auth');
  const tErrors = useTranslations('auth.errors');
  const router = useRouter();
  const forgotPassword = useForgotPassword();
  const setResetPhone = usePendingAuthStore((state) => state.setResetPhone);
  const [formError, setFormError] = useState<string | null>(null);

  const schema = useMemo(() => createForgotPasswordSchema(tErrors), [tErrors]);
  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues: { phone: '' },
  });

  function onSubmit(values: ForgotPasswordValues) {
    setFormError(null);
    forgotPassword.mutate(values, {
      onSuccess: () => {
        setResetPhone(values.phone);
        toast.info(t('forgot-password.sent'));
        router.push('/reset-password');
      },
      onError: (error) => {
        setFormError(tErrors(toAuthErrorKey(error, 'forgot-password')));
      },
    });
  }

  return (
    <AuthCard
      title={t('forgot-password.title')}
      subtitle={t('forgot-password.subtitle')}
      footer={
        <Link
          href="/login"
          className="text-muted-foreground hover:text-primary flex items-center gap-1.5 text-sm font-medium transition-colors hover:underline"
        >
          <ArrowLeft className="size-4" />
          {t('forgot-password.back-to-login')}
        </Link>
      }
    >
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} noValidate>
          <FormAlert message={formError} />
          <div className="grid gap-5">
            <motion.div variants={fieldVariants}>
              <FormField
                control={form.control}
                name="phone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.phone.label')}</FormLabel>
                    <FormControl>
                      <Input
                        type="tel"
                        inputMode="tel"
                        autoComplete="tel"
                        placeholder={t('fields.phone.placeholder')}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </motion.div>
            <motion.div variants={fieldVariants}>
              <SubmitButton pending={forgotPassword.isPending}>
                {t('forgot-password.submit')}
              </SubmitButton>
            </motion.div>
          </div>
        </form>
      </Form>
    </AuthCard>
  );
}
