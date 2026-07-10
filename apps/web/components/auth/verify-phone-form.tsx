'use client';

import { useEffect, useMemo, useState } from 'react';

import { useTranslations } from 'next-intl';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

import { zodResolver } from '@hookform/resolvers/zod';
import { REGEXP_ONLY_DIGITS } from 'input-otp';
import { motion, useAnimationControls } from 'motion/react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';

import { AuthCard, fieldVariants } from '@/components/auth/auth-card';
import { FormAlert } from '@/components/auth/form-alert';
import { SubmitButton } from '@/components/auth/submit-button';

import {
  Form,
  FormControl,
  FormDescription,
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

import { useResendOtp, useVerifyPhone } from '@/hooks/use-auth-mutations';

import { toAuthErrorKey } from '@/lib/api/api-error';
import { usePendingAuthStore } from '@/lib/auth/pending-auth-store';
import { createVerifyPhoneSchema, type VerifyPhoneValues } from '@/lib/validation/auth-schemas';
import { formatPhoneForDisplay, normalizePhone } from '@/lib/validation/normalize-phone';

const RESEND_COOLDOWN_SECONDS = 30;

export function VerifyPhoneForm() {
  const t = useTranslations('auth');
  const tErrors = useTranslations('auth.errors');
  const router = useRouter();
  const verify = useVerifyPhone();
  const resend = useResendOtp();
  const pendingHydrated = usePendingAuthStore((state) => state.hydrated);
  const verificationPhone = usePendingAuthStore((state) => state.verificationPhone);
  const setVerificationPhone = usePendingAuthStore((state) => state.setVerificationPhone);
  const [formError, setFormError] = useState<string | null>(null);
  const [cooldown, setCooldown] = useState(RESEND_COOLDOWN_SECONDS);
  const shakeControls = useAnimationControls();

  const schema = useMemo(() => createVerifyPhoneSchema(tErrors), [tErrors]);
  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues: { phone: '', code: '' },
  });

  const knownPhone = pendingHydrated ? verificationPhone : null;

  useEffect(() => {
    if (knownPhone) form.setValue('phone', knownPhone);
  }, [knownPhone, form]);

  useEffect(() => {
    if (cooldown <= 0) return;
    const id = setInterval(() => setCooldown((s) => s - 1), 1000);
    return () => clearInterval(id);
  }, [cooldown]);

  function onSubmit(values: VerifyPhoneValues) {
    setFormError(null);
    verify.mutate(values, {
      onSuccess: (session) => {
        setVerificationPhone(null);
        toast.success(t('verify-phone.success', { name: session.user.display_name }));
        router.push('/');
      },
      onError: (error) => {
        setFormError(tErrors(toAuthErrorKey(error, 'verify-phone')));
        form.resetField('code');
        void shakeControls.start({
          x: [0, -10, 10, -7, 7, -4, 4, 0],
          transition: { duration: 0.45 },
        });
      },
    });
  }

  function handleResend() {
    const rawPhone = form.getValues('phone');
    const phone = normalizePhone(rawPhone);
    if (!phone) {
      void form.trigger('phone');
      return;
    }
    resend.mutate(
      { phone },
      {
        onSuccess: () => {
          setCooldown(RESEND_COOLDOWN_SECONDS);
          toast.info(t('verify-phone.resend-sent'));
        },
        onError: (error) => {
          setFormError(tErrors(toAuthErrorKey(error, 'resend-otp')));
        },
      }
    );
  }

  return (
    <AuthCard
      title={t('verify-phone.title')}
      subtitle={
        knownPhone
          ? t('verify-phone.subtitle', { phone: formatPhoneForDisplay(knownPhone) })
          : t('verify-phone.subtitle-no-phone')
      }
      footer={
        <p className="text-muted-foreground text-sm">
          {t('verify-phone.wrong-phone')}{' '}
          <Link href="/register" className="text-primary font-semibold hover:underline">
            {t('register.title')}
          </Link>
        </p>
      }
    >
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} noValidate>
          <FormAlert message={formError} />
          <div className="grid gap-5">
            {!knownPhone ? (
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
            ) : null}
            <motion.div variants={fieldVariants}>
              <FormField
                control={form.control}
                name="code"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="sr-only">{t('fields.code.label')}</FormLabel>
                    <FormControl>
                      <motion.div animate={shakeControls} className="flex justify-center">
                        <InputOTP
                          maxLength={6}
                          pattern={REGEXP_ONLY_DIGITS}
                          autoFocus
                          value={field.value}
                          onChange={field.onChange}
                          onComplete={() => {
                            if (!verify.isPending) void form.handleSubmit(onSubmit)();
                          }}
                          disabled={verify.isPending}
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
                      </motion.div>
                    </FormControl>
                    <FormDescription className="text-center">
                      {t('verify-phone.code-hint')}
                    </FormDescription>
                    <FormMessage className="text-center" />
                  </FormItem>
                )}
              />
            </motion.div>
            <motion.div variants={fieldVariants}>
              <SubmitButton pending={verify.isPending}>{t('verify-phone.submit')}</SubmitButton>
            </motion.div>
            <motion.div variants={fieldVariants} className="text-center">
              <button
                type="button"
                onClick={handleResend}
                disabled={cooldown > 0 || resend.isPending}
                className="text-muted-foreground enabled:hover:text-primary text-sm font-medium transition-colors enabled:hover:underline disabled:opacity-60"
              >
                {cooldown > 0
                  ? t('verify-phone.resend-in', { seconds: cooldown })
                  : t('verify-phone.resend')}
              </button>
            </motion.div>
          </div>
        </form>
      </Form>
    </AuthCard>
  );
}
