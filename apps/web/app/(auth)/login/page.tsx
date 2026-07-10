import { getTranslations } from 'next-intl/server';

import { LoginForm } from '@/components/auth/login-form';

export async function generateMetadata() {
  const t = await getTranslations('auth.login');
  return { title: t('title') };
}

export default function LoginPage() {
  return <LoginForm />;
}
