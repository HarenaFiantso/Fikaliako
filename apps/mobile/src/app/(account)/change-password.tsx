import { useEffect, useState } from 'react';

import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'expo-router';
import { Controller, useForm } from 'react-hook-form';

import { AlertBanner } from '@/components/ui/alert-banner';
import { Button } from '@/components/ui/button';
import { FormScreen } from '@/components/ui/form-screen';
import { TextField } from '@/components/ui/text-field';

import { changePasswordSchema, type ChangePasswordValues } from '@/lib/auth/schemas';
import { useSession } from '@/lib/auth/session-store';
import { safeBack } from '@/lib/navigation';

export default function ChangePasswordScreen() {
  const router = useRouter();

  const changePassword = useSession((state) => state.changePassword);

  const [formError, setFormError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  useEffect(() => {
    if (!done) return;
    const timer = setTimeout(() => safeBack(router, '/profile'), 1200);
    return () => clearTimeout(timer);
  }, [done, router]);

  const {
    control,
    handleSubmit,
    formState: { isSubmitting },
  } = useForm<ChangePasswordValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: '', newPassword: '' },
  });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await changePassword(values);
      setDone(true);
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Could not change the password');
    }
  });

  return (
    <FormScreen
      title="Change password"
      subtitle="Every other device will be signed out; this one stays connected."
    >
      {formError && <AlertBanner kind="error" message={formError} />}
      {done && <AlertBanner kind="success" message="Password changed." />}
      <Controller
        control={control}
        name="currentPassword"
        render={({ field, fieldState }) => (
          <TextField
            label="Current password"
            placeholder="Your current password"
            secure
            autoComplete="current-password"
            autoCapitalize="none"
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            error={fieldState.error?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="newPassword"
        render={({ field, fieldState }) => (
          <TextField
            label="New password"
            placeholder="At least 8 characters"
            secure
            autoComplete="new-password"
            autoCapitalize="none"
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            onSubmitEditing={() => void submit()}
            error={fieldState.error?.message}
          />
        )}
      />
      <Button
        title="Change password"
        loading={isSubmitting}
        disabled={done}
        onPress={() => void submit()}
      />
    </FormScreen>
  );
}
