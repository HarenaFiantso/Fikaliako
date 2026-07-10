import { getTranslations } from 'next-intl/server';

import { VerifyPhoneForm } from '@/components/auth/verify-phone-form';

export async function generateMetadata() {
  const t = await getTranslations('auth.verify-phone');
  return { title: t('title') };
}

export default function VerifyPhonePage() {
  return <VerifyPhoneForm />;
}
