import { useState } from 'react';

import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'expo-router';
import { Controller, useForm } from 'react-hook-form';

import { AlertBanner } from '@/components/ui/alert-banner';
import { Button } from '@/components/ui/button';
import { FormScreen } from '@/components/ui/form-screen';
import { TextField } from '@/components/ui/text-field';

import { forgotPasswordSchema, type ForgotPasswordValues } from '@/lib/auth/schemas';
import { useSession } from '@/lib/auth/session-store';

export default function ForgotPasswordScreen() {
  const router = useRouter();
  const forgotPassword = useSession((state) => state.forgotPassword);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    formState: { isSubmitting },
  } = useForm<ForgotPasswordValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { phone: '' },
  });

  const submit = handleSubmit(async ({ phone }) => {
    setFormError(null);
    try {
      await forgotPassword(phone);
      router.push({ pathname: '/reset-password', params: { phone } });
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Could not send the code');
    }
  });

  return (
    <FormScreen
      title="Forgot your password?"
      subtitle="If this number has an account, we will text a reset code to it."
    >
      {formError && <AlertBanner kind="error" message={formError} />}

      <Controller
        control={control}
        name="phone"
        render={({ field, fieldState }) => (
          <TextField
            label="Phone number"
            placeholder="034 12 345 67"
            keyboardType="phone-pad"
            autoComplete="tel"
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            onSubmitEditing={() => void submit()}
            error={fieldState.error?.message}
          />
        )}
      />

      <Button title="Send reset code" loading={isSubmitting} onPress={() => void submit()} />
    </FormScreen>
  );
}
