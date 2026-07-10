'use client';

import { useEffect, useMemo, useState } from 'react';

import { useTranslations } from 'next-intl';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

import { zodResolver } from '@hookform/resolvers/zod';
import { REGEXP_ONLY_DIGITS } from 'input-otp';
import { ArrowLeft } from 'lucide-react';
import { motion } from 'motion/react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';

import { AuthCard, fieldVariants } from '@/components/auth/auth-card';
import { FormAlert } from '@/components/auth/form-alert';
import { PasswordInput } from '@/components/auth/password-input';
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
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSeparator,
  InputOTPSlot,
} from '@/components/ui/input-otp';

import { useResetPassword } from '@/hooks/use-auth-mutations';

import { toAuthErrorKey } from '@/lib/api/api-error';
import { usePendingAuthStore } from '@/lib/auth/pending-auth-store';
import { createResetPasswordSchema, type ResetPasswordValues } from '@/lib/validation/auth-schemas';
import { formatPhoneForDisplay } from '@/lib/validation/normalize-phone';

export function ResetPasswordForm() {
  const t = useTranslations('auth');
  const tErrors = useTranslations('auth.errors');
  const router = useRouter();
  const resetPassword = useResetPassword();
  const pendingHydrated = usePendingAuthStore((state) => state.hydrated);
  const resetPhone = usePendingAuthStore((state) => state.resetPhone);
  const setResetPhone = usePendingAuthStore((state) => state.setResetPhone);
  const [formError, setFormError] = useState<string | null>(null);

  const schema = useMemo(() => createResetPasswordSchema(tErrors), [tErrors]);
  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues: { phone: '', code: '', newPassword: '' },
  });

  const knownPhone = pendingHydrated ? resetPhone : null;

  useEffect(() => {
    if (knownPhone) form.setValue('phone', formatPhoneForDisplay(knownPhone));
  }, [knownPhone, form]);

  function onSubmit(values: ResetPasswordValues) {
    setFormError(null);
    resetPassword.mutate(
      { phone: values.phone, code: values.code, new_password: values.newPassword },
      {
        onSuccess: () => {
          setResetPhone(null);
          toast.success(t('reset-password.success'));
          router.push('/login');
        },
        onError: (error) => {
          setFormError(tErrors(toAuthErrorKey(error, 'reset-password')));
        },
      }
    );
  }

  return (
    <AuthCard
      title={t('reset-password.title')}
      subtitle={t('reset-password.subtitle')}
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
              <FormField
                control={form.control}
                name="code"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.code.label')}</FormLabel>
                    <FormControl>
                      <InputOTP
                        maxLength={6}
                        pattern={REGEXP_ONLY_DIGITS}
                        value={field.value}
                        onChange={field.onChange}
                        disabled={resetPassword.isPending}
                      >
                        <InputOTPGroup>
                          <InputOTPSlot index={0} />
                          <InputOTPSlot index={1} />
                          <InputOTPSlot index={2} />
                        </InputOTPGroup>
                        <InputOTPSeparator />
                        <InputOTPGroup>
                          <InputOTPSlot index={3} />
                          <InputOTPSlot index={4} />
                          <InputOTPSlot index={5} />
                        </InputOTPGroup>
                      </InputOTP>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </motion.div>
            <motion.div variants={fieldVariants}>
              <FormField
                control={form.control}
                name="newPassword"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.new-password.label')}</FormLabel>
                    <FormControl>
                      <PasswordInput
                        autoComplete="new-password"
                        placeholder={t('fields.password.placeholder')}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </motion.div>
            <motion.div variants={fieldVariants}>
              <SubmitButton pending={resetPassword.isPending}>
                {t('reset-password.submit')}
              </SubmitButton>
            </motion.div>
          </div>
        </form>
      </Form>
    </AuthCard>
  );
}
