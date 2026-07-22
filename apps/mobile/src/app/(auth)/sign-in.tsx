import { useState } from 'react';

import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useLocalSearchParams, useRouter } from 'expo-router';
import { Controller, useForm } from 'react-hook-form';
import { StyleSheet, View } from 'react-native';

import { AlertBanner } from '@/components/ui/alert-banner';
import { Button } from '@/components/ui/button';
import { FormScreen } from '@/components/ui/form-screen';
import { TextField } from '@/components/ui/text-field';

import { ThemedText } from '@/components/themed-text';

import { AuthError } from '@/lib/auth/errors';
import { signInSchema, type SignInValues } from '@/lib/auth/schemas';
import { useSession } from '@/lib/auth/session-store';

import { Spacing } from '@/constants/theme';

export default function SignInScreen() {
  const router = useRouter();
  const { reset } = useLocalSearchParams<{ reset?: string }>();
  const signIn = useSession((state) => state.signIn);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    formState: { isSubmitting },
  } = useForm<SignInValues>({
    resolver: zodResolver(signInSchema),
    defaultValues: { phone: '', password: '' },
  });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await signIn(values);
      router.dismissTo('/profile');
    } catch (error) {
      if (error instanceof AuthError && error.kind === 'phoneUnverified') {
        router.push({ pathname: '/verify-phone', params: { phone: values.phone } });
        return;
      }
      setFormError(error instanceof Error ? error.message : 'Could not sign in');
    }
  });

  return (
    <FormScreen title="Welcome back" subtitle="Sign in to save favorites, review and contribute.">
      {reset === '1' && !formError && (
        <AlertBanner kind="success" message="Password reset. Sign in with your new password." />
      )}
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
            placeholder="Your password"
            secure
            autoComplete="current-password"
            autoCapitalize="none"
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            onSubmitEditing={() => void submit()}
            error={fieldState.error?.message}
          />
        )}
      />

      <Link href="/forgot-password" style={styles.forgotLink}>
        <ThemedText type="linkPrimary">Forgot password?</ThemedText>
      </Link>

      <Button title="Sign in" loading={isSubmitting} onPress={() => void submit()} />

      <View style={styles.footer}>
        <ThemedText type="small" themeColor="textSecondary">
          New to Fikaliako?
        </ThemedText>
        <Link href="/sign-up" replace>
          <ThemedText type="linkPrimary">Create an account</ThemedText>
        </Link>
      </View>
    </FormScreen>
  );
}

const styles = StyleSheet.create({
  forgotLink: {
    alignSelf: 'flex-end',
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    gap: Spacing.two,
    marginTop: Spacing.three,
  },
});
