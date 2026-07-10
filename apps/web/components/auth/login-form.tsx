'use client';

import { useMemo, useState } from 'react';

import { useTranslations } from 'next-intl';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

import { zodResolver } from '@hookform/resolvers/zod';
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

import { useLogin } from '@/hooks/use-auth-mutations';

import { toAuthErrorKey } from '@/lib/api/api-error';
import { usePendingAuthStore } from '@/lib/auth/pending-auth-store';
import { createLoginSchema, type LoginValues } from '@/lib/validation/auth-schemas';

export function LoginForm() {
  const t = useTranslations('auth');
  const tErrors = useTranslations('auth.errors');
  const router = useRouter();
  const login = useLogin();
  const setVerificationPhone = usePendingAuthStore((state) => state.setVerificationPhone);
  const [formError, setFormError] = useState<string | null>(null);

  const schema = useMemo(() => createLoginSchema(tErrors), [tErrors]);
  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues: { phone: '', password: '' },
  });

  function onSubmit(values: LoginValues) {
    setFormError(null);
    login.mutate(values, {
      onSuccess: (session) => {
        toast.success(t('login.success', { name: session.user.display_name }));
        router.push('/');
      },
      onError: (error) => {
        const key = toAuthErrorKey(error, 'login');
        if (key === 'phone-not-verified') {
          setVerificationPhone(values.phone);
          toast.info(tErrors(key));
          router.push('/verify-phone');
          return;
        }
        setFormError(tErrors(key));
      },
    });
  }

  return (
    <AuthCard
      title={t('login.title')}
      subtitle={t('login.subtitle')}
      footer={
        <p className="text-muted-foreground text-sm">
          {t('login.no-account')}{' '}
          <Link href="/register" className="text-primary font-semibold hover:underline">
            {t('login.register-link')}
          </Link>
        </p>
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
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex items-center justify-between">
                      <FormLabel>{t('fields.password.label')}</FormLabel>
                      <Link
                        href="/forgot-password"
                        className="text-muted-foreground hover:text-primary text-xs font-medium transition-colors hover:underline"
                      >
                        {t('login.forgot-password')}
                      </Link>
                    </div>
                    <FormControl>
                      <PasswordInput
                        autoComplete="current-password"
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
              <SubmitButton pending={login.isPending}>{t('login.submit')}</SubmitButton>
            </motion.div>
          </div>
        </form>
      </Form>
    </AuthCard>
  );
}
