import { useState } from 'react';

import { zodResolver } from '@hookform/resolvers/zod';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Controller, useForm } from 'react-hook-form';

import { AlertBanner } from '@/components/ui/alert-banner';
import { Button } from '@/components/ui/button';
import { FormScreen } from '@/components/ui/form-screen';
import { OtpInput } from '@/components/ui/otp-input';
import { TextField } from '@/components/ui/text-field';

import { formatPhone, normalizePhone } from '@/lib/auth/phone';
import { resetPasswordSchema, type ResetPasswordValues } from '@/lib/auth/schemas';
import { useSession } from '@/lib/auth/session-store';

export default function ResetPasswordScreen() {
  const router = useRouter();
  const { phone = '' } = useLocalSearchParams<{ phone?: string }>();
  const resetPassword = useSession((state) => state.resetPassword);
  const [formError, setFormError] = useState<string | null>(null);

  const displayPhone = formatPhone(normalizePhone(phone) ?? phone);

  const {
    control,
    handleSubmit,
    formState: { isSubmitting },
  } = useForm<ResetPasswordValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { code: '', newPassword: '' },
  });

  const submit = handleSubmit(async ({ code, newPassword }) => {
    setFormError(null);
    try {
      await resetPassword({ phone, code, newPassword });
      router.dismissTo({ pathname: '/sign-in', params: { reset: '1' } });
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Could not reset the password');
    }
  });

  return (
    <FormScreen
      title="Reset your password"
      subtitle={`Enter the code sent to ${displayPhone} and pick a new password.`}
    >
      {formError && <AlertBanner kind="error" message={formError} />}

      <Controller
        control={control}
        name="code"
        render={({ field, fieldState }) => (
          <OtpInput
            value={field.value}
            onChange={field.onChange}
            error={fieldState.error?.message}
            autoFocus
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

      <Button title="Reset password" loading={isSubmitting} onPress={() => void submit()} />
    </FormScreen>
  );
}
