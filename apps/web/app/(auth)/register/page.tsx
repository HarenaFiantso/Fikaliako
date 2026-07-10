import { getTranslations } from 'next-intl/server';

import { RegisterForm } from '@/components/auth/register-form';

export async function generateMetadata() {
  const t = await getTranslations('auth.register');
  return { title: t('title') };
}

export default function RegisterPage() {
  return <RegisterForm />;
}
