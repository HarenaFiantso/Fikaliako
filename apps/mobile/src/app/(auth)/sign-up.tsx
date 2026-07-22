import { useState } from 'react';

import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useRouter } from 'expo-router';
import { Controller, useForm } from 'react-hook-form';
import { StyleSheet, View } from 'react-native';

import { AlertBanner } from '@/components/ui/alert-banner';
import { Button } from '@/components/ui/button';
import { FormScreen } from '@/components/ui/form-screen';
import { SegmentedControl } from '@/components/ui/segmented-control';
import { TextField } from '@/components/ui/text-field';

import { ThemedText } from '@/components/themed-text';

import { signUpSchema, type SignUpValues } from '@/lib/auth/schemas';
import { useSession } from '@/lib/auth/session-store';

import { Spacing } from '@/constants/theme';

const ACCOUNT_TYPES = [
  { value: 'consumer', label: 'I want to eat' },
  { value: 'business', label: 'I run a place' },
] as const;

export default function SignUpScreen() {
  const router = useRouter();
  const register = useSession((state) => state.register);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    formState: { isSubmitting },
  } = useForm<SignUpValues>({
    resolver: zodResolver(signUpSchema),
    defaultValues: { displayName: '', phone: '', password: '', accountType: 'consumer' },
  });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      const phone = await register(values);
      router.push({ pathname: '/verify-phone', params: { phone } });
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Could not create the account');
    }
  });

  return (
    <FormScreen
      title="Create an account"
      subtitle="A phone number is all it takes to join Fikaliako."
    >
      {formError && <AlertBanner kind="error" message={formError} />}

      <Controller
        control={control}
        name="accountType"
        render={({ field }) => (
          <SegmentedControl options={ACCOUNT_TYPES} value={field.value} onChange={field.onChange} />
        )}
      />
      <Controller
        control={control}
        name="displayName"
        render={({ field, fieldState }) => (
          <TextField
            label="Display name"
            placeholder="How should we call you?"
            autoComplete="name"
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            error={fieldState.error?.message}
          />
        )}
      />
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
            error={fieldState.error?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="password"
        render={({ field, fieldState }) => (
          <TextField
            label="Password"
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

      <Button title="Create account" loading={isSubmitting} onPress={() => void submit()} />

      <View style={styles.footer}>
        <ThemedText type="small" themeColor="textSecondary">
          Already have an account?
        </ThemedText>
        <Link href="/sign-in" replace>
          <ThemedText type="linkPrimary">Sign in</ThemedText>
        </Link>
      </View>
    </FormScreen>
  );
}

const styles = StyleSheet.create({
  footer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    gap: Spacing.two,
    marginTop: Spacing.three,
  },
});
