import { useState } from 'react';

import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'expo-router';
import { Controller, useForm } from 'react-hook-form';
import { StyleSheet, View } from 'react-native';

import { AlertBanner } from '@/components/ui/alert-banner';
import { Button } from '@/components/ui/button';
import { FormScreen } from '@/components/ui/form-screen';
import { SegmentedControl } from '@/components/ui/segmented-control';
import { TextField } from '@/components/ui/text-field';

import { ThemedText } from '@/components/themed-text';

import { editProfileSchema, type EditProfileValues } from '@/lib/auth/schemas';
import { useSession } from '@/lib/auth/session-store';

import { Spacing } from '@/constants/theme';

const LANGUAGES = [
  { value: 'fr', label: 'Français' },
  { value: 'mg', label: 'Malagasy' },
] as const;

export default function EditProfileScreen() {
  const router = useRouter();

  const user = useSession((state) => state.user);
  const updateProfile = useSession((state) => state.updateProfile);

  const [formError, setFormError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  const {
    control,
    handleSubmit,
    formState: { isSubmitting },
  } = useForm<EditProfileValues>({
    resolver: zodResolver(editProfileSchema),
    defaultValues: {
      displayName: user?.display_name ?? '',
      locale: user?.locale ?? 'fr',
    },
  });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await updateProfile(values);
      setDone(true);
      setTimeout(() => router.back(), 1200);
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Could not update the profile');
    }
  });

  return (
    <FormScreen title="Edit profile" subtitle="How you appear in your reviews and contributions.">
      {formError && <AlertBanner kind="error" message={formError} />}
      {done && <AlertBanner kind="success" message="Profile updated." />}
      <Controller
        control={control}
        name="displayName"
        render={({ field, fieldState }) => (
          <TextField
            label="Display name"
            placeholder="Your public name"
            autoComplete="name"
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            onSubmitEditing={() => void submit()}
            error={fieldState.error?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="locale"
        render={({ field }) => (
          <View style={styles.field}>
            <ThemedText type="smallBold">Language</ThemedText>
            <SegmentedControl options={LANGUAGES} value={field.value} onChange={field.onChange} />
          </View>
        )}
      />
      <Button
        title="Save changes"
        loading={isSubmitting}
        disabled={done}
        onPress={() => void submit()}
      />
    </FormScreen>
  );
}

const styles = StyleSheet.create({
  field: {
    gap: Spacing.two,
  },
});
