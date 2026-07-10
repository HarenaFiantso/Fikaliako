'use client';

import { useMemo, useState } from 'react';

import { useLocale, useTranslations } from 'next-intl';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

import { zodResolver } from '@hookform/resolvers/zod';
import { motion } from 'motion/react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';

import { AccountTypeSelector } from '@/components/auth/account-type-selector';
import { AuthCard, fieldVariants } from '@/components/auth/auth-card';
import { FormAlert } from '@/components/auth/form-alert';
import { PasswordInput } from '@/components/auth/password-input';
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

import { useRegister } from '@/hooks/use-auth-mutations';

import { toAuthErrorKey } from '@/lib/api/api-error';
import { usePendingAuthStore } from '@/lib/auth/pending-auth-store';
import { createRegisterSchema, type RegisterValues } from '@/lib/validation/auth-schemas';

import { type Locale, toApiLocale } from '@/i18n/config';

export function RegisterForm() {
  const t = useTranslations('auth');
  const tErrors = useTranslations('auth.errors');
  const locale = useLocale() as Locale;
  const router = useRouter();
  const register = useRegister();
  const setVerificationPhone = usePendingAuthStore((state) => state.setVerificationPhone);
  const [formError, setFormError] = useState<string | null>(null);

  const schema = useMemo(() => createRegisterSchema(tErrors), [tErrors]);
  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues: {
      displayName: '',
      phone: '',
      password: '',
      accountType: 'consumer' as const,
    },
  });

  function onSubmit(values: RegisterValues) {
    setFormError(null);
    register.mutate(
      {
        phone: values.phone,
        password: values.password,
        display_name: values.displayName,
        account_type: values.accountType,
        locale: toApiLocale(locale),
      },
      {
        onSuccess: () => {
          setVerificationPhone(values.phone);
          toast.success(t('register.success'));
          router.push('/verify-phone');
        },
        onError: (error) => {
          setFormError(tErrors(toAuthErrorKey(error, 'register')));
        },
      }
    );
  }

  return (
    <AuthCard
      title={t('register.title')}
      subtitle={t('register.subtitle')}
      footer={
        <p className="text-muted-foreground text-sm">
          {t('register.have-account')}{' '}
          <Link href="/login" className="text-primary font-semibold hover:underline">
            {t('register.login-link')}
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
                name="accountType"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.account-type.label')}</FormLabel>
                    <FormControl>
                      <AccountTypeSelector value={field.value} onChange={field.onChange} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </motion.div>
            <motion.div variants={fieldVariants}>
              <FormField
                control={form.control}
                name="displayName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.display-name.label')}</FormLabel>
                    <FormControl>
                      <Input
                        autoComplete="nickname"
                        placeholder={t('fields.display-name.placeholder')}
                        {...field}
                      />
                    </FormControl>
                    <FormDescription>{t('fields.display-name.description')}</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </motion.div>
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
                    <FormDescription>{t('fields.phone.description')}</FormDescription>
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
                    <FormLabel>{t('fields.password.label')}</FormLabel>
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
              <SubmitButton pending={register.isPending}>{t('register.submit')}</SubmitButton>
            </motion.div>
          </div>
        </form>
      </Form>
    </AuthCard>
  );
}
