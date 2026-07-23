import { useEffect, useState } from 'react';

import { useLocalSearchParams, useRouter } from 'expo-router';
import { StyleSheet, TextInput, View } from 'react-native';

import { AlertBanner } from '@/components/ui/alert-banner';
import { Button } from '@/components/ui/button';
import { FormScreen } from '@/components/ui/form-screen';
import { StarRatingInput } from '@/components/ui/star-rating';

import { ThemedText } from '@/components/themed-text';

import { useTheme } from '@/hooks/use-theme';

import { safeBack } from '@/lib/navigation';
import { useReviews } from '@/lib/reviews-store';

import { FontFamily, Radius, Spacing } from '@/constants/theme';

const CRITERIA = [
  { key: 'quality', label: 'Quality' },
  { key: 'price', label: 'Price' },
  { key: 'cleanliness', label: 'Cleanliness' },
  { key: 'speed', label: 'Speed' },
  { key: 'welcome', label: 'Welcome' },
] as const;

type CriterionKey = (typeof CRITERIA)[number]['key'];

const UNRATED: Record<CriterionKey, number> = {
  quality: 0,
  price: 0,
  cleanliness: 0,
  speed: 0,
  welcome: 0,
};

export default function ReviewScreen() {
  const router = useRouter();
  const theme = useTheme();

  const { establishmentId, name } = useLocalSearchParams<{
    establishmentId: string;
    name?: string;
  }>();

  const submit = useReviews((state) => state.submit);

  const [ratings, setRatings] = useState(UNRATED);
  const [comment, setComment] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);
  const [done, setDone] = useState(false);

  useEffect(() => {
    if (!done) return;
    const timer = setTimeout(() => safeBack(router), 1200);
    return () => clearTimeout(timer);
  }, [done, router]);

  const onSubmit = async () => {
    if (!establishmentId) return;
    if (CRITERIA.some((criterion) => ratings[criterion.key] === 0)) {
      setFormError('Rate all five criteria before publishing.');
      return;
    }
    setFormError(null);
    setSending(true);
    try {
      await submit(establishmentId, {
        rating_quality: ratings.quality,
        rating_price: ratings.price,
        rating_cleanliness: ratings.cleanliness,
        rating_speed: ratings.speed,
        rating_welcome: ratings.welcome,
        ...(comment.trim() ? { comment: comment.trim() } : {}),
      });
      setDone(true);
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Could not send the review');
    } finally {
      setSending(false);
    }
  };

  return (
    <FormScreen title="Share your experience" subtitle={name ? `How was ${name}?` : 'How was it?'}>
      {formError && <AlertBanner kind="error" message={formError} />}
      {done && <AlertBanner kind="success" message="Misaotra! Your review is published." />}

      <View style={styles.criteria}>
        {CRITERIA.map((criterion) => (
          <StarRatingInput
            key={criterion.key}
            label={criterion.label}
            value={ratings[criterion.key]}
            onChange={(value) =>
              setRatings((previous) => ({ ...previous, [criterion.key]: value }))
            }
          />
        ))}
      </View>

      <View style={styles.commentField}>
        <ThemedText type="smallBold">Comment (optional)</ThemedText>
        <TextInput
          multiline
          maxLength={2000}
          placeholder="Tell others what to expect…"
          placeholderTextColor={theme.textSecondary}
          value={comment}
          onChangeText={setComment}
          style={[
            styles.commentInput,
            { backgroundColor: theme.card, borderColor: theme.border, color: theme.text },
          ]}
        />
      </View>

      <Button
        title="Publish review"
        loading={sending}
        disabled={done}
        onPress={() => void onSubmit()}
      />
    </FormScreen>
  );
}

const styles = StyleSheet.create({
  criteria: {
    gap: Spacing.two,
  },
  commentField: {
    gap: Spacing.two,
  },
  commentInput: {
    borderWidth: 1,
    borderRadius: Radius.xl,
    minHeight: 120,
    padding: Spacing.three,
    fontFamily: FontFamily.medium,
    fontSize: 16,
    textAlignVertical: 'top',
  },
});
