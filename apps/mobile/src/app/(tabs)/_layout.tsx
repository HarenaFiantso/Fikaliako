import { Redirect } from 'expo-router';

import AppTabs from '@/components/app-tabs';

import { useOnboarding } from '@/lib/onboarding-store';

export default function TabsLayout() {
  const ready = useOnboarding((state) => state.ready);
  const completed = useOnboarding((state) => state.completed);

  if (!ready) return null;
  if (!completed) return <Redirect href="/welcome" />;
  return <AppTabs />;
}
