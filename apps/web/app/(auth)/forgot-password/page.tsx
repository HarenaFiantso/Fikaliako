import { getTranslations } from 'next-intl/server';

import { ForgotPasswordForm } from '@/components/auth/forgot-password-form';

export async function generateMetadata() {
  const t = await getTranslations('auth.forgot-password');
  return { title: t('title') };
}

export default function ForgotPasswordPage() {
  return <ForgotPasswordForm />;
}
