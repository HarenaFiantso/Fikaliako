import { getTranslations } from 'next-intl/server';

import { ResetPasswordForm } from '@/components/auth/reset-password-form';

export async function generateMetadata() {
  const t = await getTranslations('auth.reset-password');
  return { title: t('title') };
}

export default function ResetPasswordPage() {
  return <ResetPasswordForm />;
}
