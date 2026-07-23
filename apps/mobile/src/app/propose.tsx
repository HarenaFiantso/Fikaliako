import { useEffect, useState } from 'react';

import * as Location from 'expo-location';
import { Ionicons } from '@expo/vector-icons';
import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'expo-router';
import { Controller, useForm } from 'react-hook-form';
import { Pressable, StyleSheet, TextInput, View } from 'react-native';

import { AlertBanner } from '@/components/ui/alert-banner';
import { Button } from '@/components/ui/button';
import { FormScreen } from '@/components/ui/form-screen';
import { TextField } from '@/components/ui/text-field';

import { TYPE_META } from '@/components/establishment-card';
import { ThemedText } from '@/components/themed-text';

import { useTheme } from '@/hooks/use-theme';

import {
  ESTABLISHMENT_TYPES,
  proposalSchema,
  type ProposalValues,
  submitProposal,
} from '@/lib/contributions';
import { safeBack } from '@/lib/navigation';

import { FontFamily, Radius, Spacing } from '@/constants/theme';

interface Pin {
  lat: number;
  lng: number;
  accuracy: number | null;
}

export default function ProposeScreen() {
  const router = useRouter();
  const theme = useTheme();

  const [pin, setPin] = useState<Pin | null>(null);
  const [locating, setLocating] = useState(false);
  const [locationError, setLocationError] = useState<string | null>(null);

  const [formError, setFormError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  const {
    control,
    handleSubmit,
    formState: { isSubmitting },
  } = useForm<ProposalValues>({
    resolver: zodResolver(proposalSchema),
    defaultValues: {
      name: '',
      type: 'gargotte',
      city: 'Antananarivo',
      district: '',
      address: '',
      phone: '',
      avgPrice: '',
      comment: '',
    },
  });

  useEffect(() => {
    if (!done) return;
    const timer = setTimeout(() => safeBack(router, '/explore'), 1600);
    return () => clearTimeout(timer);
  }, [done, router]);

  const dropPin = async () => {
    setLocating(true);
    setLocationError(null);
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        setLocationError('Location permission is needed to drop the pin where you stand.');
        return;
      }
      const { coords } = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.Balanced,
      });
      setPin({ lat: coords.latitude, lng: coords.longitude, accuracy: coords.accuracy ?? null });
    } catch {
      setLocationError('Could not read your position. Try again with a clearer sky view.');
    } finally {
      setLocating(false);
    }
  };

  const submit = handleSubmit(async (values) => {
    if (!pin) {
      setFormError('Drop the pin first — the position is what puts a place on the map.');
      return;
    }
    setFormError(null);
    try {
      await submitProposal(values, { lat: pin.lat, lng: pin.lng });
      setDone(true);
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Could not send the proposal');
    }
  });

  return (
    <FormScreen
      title="Add a place"
      subtitle="Standing somewhere good that Fikaliako is missing? Put it on the map."
    >
      {formError && <AlertBanner kind="error" message={formError} />}
      {done && (
        <AlertBanner kind="success" message="Misaotra! Your proposal is off to moderation." />
      )}

      <Controller
        control={control}
        name="type"
        render={({ field }) => (
          <View style={styles.field}>
            <ThemedText type="smallBold">What kind of place?</ThemedText>
            <View style={styles.typeWrap}>
              {ESTABLISHMENT_TYPES.map((type) => {
                const meta = TYPE_META[type];
                const selected = field.value === type;
                return (
                  <Pressable
                    key={type}
                    accessibilityRole="button"
                    onPress={() => field.onChange(type)}
                    style={[
                      styles.typeChip,
                      { backgroundColor: selected ? theme.accent : theme.backgroundElement },
                    ]}
                  >
                    <Ionicons
                      name={meta.icon}
                      size={15}
                      color={selected ? theme.primary : theme.textSecondary}
                    />
                    <ThemedText
                      type={selected ? 'smallBold' : 'small'}
                      style={{ color: selected ? theme.primary : theme.textSecondary }}
                    >
                      {meta.label}
                    </ThemedText>
                  </Pressable>
                );
              })}
            </View>
          </View>
        )}
      />

      <Controller
        control={control}
        name="name"
        render={({ field, fieldState }) => (
          <TextField
            label="Name"
            placeholder="Chez Mama Fara"
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            error={fieldState.error?.message}
          />
        )}
      />

      <View style={styles.field}>
        <ThemedText type="smallBold">Position</ThemedText>
        <View style={[styles.pinCard, { backgroundColor: theme.card, borderColor: theme.border }]}>
          <Ionicons
            name={pin ? 'location' : 'location-outline'}
            size={22}
            color={pin ? theme.primary : theme.textSecondary}
          />
          <View style={styles.pinText}>
            {pin ? (
              <>
                <ThemedText type="smallBold">Pin dropped</ThemedText>
                <ThemedText type="small" themeColor="textSecondary">
                  {pin.lat.toFixed(5)}, {pin.lng.toFixed(5)}
                  {pin.accuracy != null ? ` · ±${Math.round(pin.accuracy)} m` : ''}
                </ThemedText>
              </>
            ) : (
              <ThemedText type="small" themeColor="textSecondary">
                Stand at the entrance and drop the pin — that is what places it on the map.
              </ThemedText>
            )}
          </View>
        </View>
        {locationError && (
          <ThemedText type="small" style={{ color: theme.danger }}>
            {locationError}
          </ThemedText>
        )}
        <Button
          title={pin ? 'Update the pin' : 'Drop the pin here'}
          variant="secondary"
          loading={locating}
          onPress={() => void dropPin()}
        />
      </View>

      <Controller
        control={control}
        name="district"
        render={({ field, fieldState }) => (
          <TextField
            label="District (optional)"
            placeholder="Analakely"
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            error={fieldState.error?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="city"
        render={({ field, fieldState }) => (
          <TextField
            label="City"
            placeholder="Antananarivo"
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            error={fieldState.error?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="address"
        render={({ field, fieldState }) => (
          <TextField
            label="Address or landmark (optional)"
            placeholder="Near the Analakely market stairs"
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
            label="Phone (optional)"
            placeholder="034 12 345 67"
            keyboardType="phone-pad"
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            error={fieldState.error?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="avgPrice"
        render={({ field, fieldState }) => (
          <TextField
            label="Average price in Ar (optional)"
            placeholder="3000"
            keyboardType="number-pad"
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            error={fieldState.error?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="comment"
        render={({ field, fieldState }) => (
          <View style={styles.field}>
            <ThemedText type="smallBold">Note for the moderators (optional)</ThemedText>
            <TextInput
              multiline
              maxLength={500}
              placeholder="Specialities, opening habits, how to spot it…"
              placeholderTextColor={theme.textSecondary}
              value={field.value}
              onChangeText={field.onChange}
              onBlur={field.onBlur}
              style={[
                styles.commentInput,
                { backgroundColor: theme.card, borderColor: theme.border, color: theme.text },
              ]}
            />
            {fieldState.error?.message && (
              <ThemedText type="small" style={{ color: theme.danger }}>
                {fieldState.error.message}
              </ThemedText>
            )}
          </View>
        )}
      />

      <Button
        title="Send for moderation"
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
  typeWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.two,
  },
  typeChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.one,
    borderRadius: Radius.full,
    paddingHorizontal: Spacing.three,
    minHeight: 36,
  },
  pinCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    borderWidth: 1,
    borderRadius: Radius.xxl,
    padding: Spacing.three,
  },
  pinText: {
    flex: 1,
    gap: Spacing.half,
  },
  commentInput: {
    borderWidth: 1,
    borderRadius: Radius.xl,
    minHeight: 100,
    padding: Spacing.three,
    fontFamily: FontFamily.medium,
    fontSize: 16,
    textAlignVertical: 'top',
  },
});
