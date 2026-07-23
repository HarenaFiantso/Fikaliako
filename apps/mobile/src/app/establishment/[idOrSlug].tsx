import { type ReactNode, useEffect } from 'react';

import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { ActivityIndicator, Linking, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import Animated, { FadeInDown } from 'react-native-reanimated';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import type { Amenities, EstablishmentDetail, EstablishmentSummary } from '@fikaliako/api-client';

import { Button } from '@/components/ui/button';
import { StarRow } from '@/components/ui/star-rating';

import { TYPE_META } from '@/components/establishment-card';
import { FavoriteButton } from '@/components/favorite-button';
import { OpeningHours } from '@/components/opening-hours';
import { RatingBreakdown } from '@/components/rating-breakdown';
import { ReviewCard } from '@/components/review-card';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';

import { useTheme } from '@/hooks/use-theme';

import { useEstablishmentDetail } from '@/lib/establishment-store';
import { useFavorites } from '@/lib/favorites-store';
import { formatAriary } from '@/lib/format';
import { safeBack } from '@/lib/navigation';
import { useReviews } from '@/lib/reviews-store';

import { MaxContentWidth, Radius, Spacing } from '@/constants/theme';

const AMENITY_META: {
  key: keyof Amenities;
  label: string;
  icon: keyof typeof Ionicons.glyphMap;
}[] = [
  { key: 'delivery', label: 'Delivery', icon: 'bicycle-outline' },
  { key: 'parking', label: 'Parking', icon: 'car-outline' },
  { key: 'wifi', label: 'Wi-Fi', icon: 'wifi-outline' },
  { key: 'wheelchair_access', label: 'Wheelchair access', icon: 'accessibility-outline' },
  { key: 'air_conditioning', label: 'Air conditioning', icon: 'snow-outline' },
  { key: 'terrace', label: 'Terrace', icon: 'sunny-outline' },
  { key: 'family_friendly', label: 'Family friendly', icon: 'people-outline' },
  { key: 'romantic', label: 'Romantic', icon: 'flower-outline' },
  { key: 'student_friendly', label: 'Student friendly', icon: 'school-outline' },
  { key: 'scenic_view', label: 'Scenic view', icon: 'image-outline' },
  { key: 'open_24h', label: 'Open 24h', icon: 'time-outline' },
];

function paymentIcon(code: string): keyof typeof Ionicons.glyphMap {
  if (code === 'cash') return 'cash-outline';
  if (code === 'carte') return 'card-outline';
  return 'phone-portrait-outline';
}

export default function EstablishmentScreen() {
  const { idOrSlug } = useLocalSearchParams<{ idOrSlug: string }>();
  const key = idOrSlug ?? '';

  const router = useRouter();
  const theme = useTheme();
  const insets = useSafeAreaInsets();

  const entry = useEstablishmentDetail((state) => state.entries[key]);
  const load = useEstablishmentDetail((state) => state.load);
  const ensureLoaded = useEstablishmentDetail((state) => state.ensureLoaded);

  const ensureFavorites = useFavorites((state) => state.ensureLoaded);

  useEffect(() => {
    if (key) ensureLoaded(key);
    ensureFavorites();
  }, [key, ensureLoaded, ensureFavorites]);

  const detail = entry?.data;

  const floatingTop = insets.top + Spacing.two;

  return (
    <ThemedView style={styles.root}>
      {!detail ? (
        <View style={styles.centered}>
          {entry?.status === 'error' ? (
            <View style={styles.stateBlock}>
              <ThemedText type="default" themeColor="textSecondary" style={styles.centeredText}>
                This place could not be loaded.
              </ThemedText>
              <Button title="Try again" variant="secondary" onPress={() => void load(key)} />
            </View>
          ) : (
            <ActivityIndicator color={theme.primary} />
          )}
        </View>
      ) : (
        <DetailContent detail={detail} />
      )}

      <View style={[styles.floatingBar, { top: floatingTop }]} pointerEvents="box-none">
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Go back"
          onPress={() => safeBack(router, '/explore')}
          style={[
            styles.floatingButton,
            { backgroundColor: theme.card, borderColor: theme.border },
          ]}
        >
          <Ionicons name="chevron-back" size={22} color={theme.text} />
        </Pressable>
        {detail && (
          <View
            style={[
              styles.floatingButton,
              { backgroundColor: theme.card, borderColor: theme.border },
            ]}
          >
            <FavoriteButton establishment={toSummary(detail)} />
          </View>
        )}
      </View>
    </ThemedView>
  );
}

function toSummary(detail: EstablishmentDetail): EstablishmentSummary {
  return {
    id: detail.id,
    slug: detail.slug,
    name: detail.name,
    type: detail.type,
    position: detail.position,
    verified: detail.verified,
    status: detail.status,
    rating_count: detail.rating.count,
    ...(detail.avg_price_ar != null && { avg_price_ar: detail.avg_price_ar }),
    ...(detail.rating.avg_global != null && { rating_avg: detail.rating.avg_global }),
  };
}

function DetailContent({ detail }: { detail: EstablishmentDetail }) {
  const theme = useTheme();
  const insets = useSafeAreaInsets();

  const reviewsEntry = useReviews((state) => state.entries[detail.id]);
  const ensureReviews = useReviews((state) => state.ensureLoaded);
  const loadReviews = useReviews((state) => state.load);
  const loadMoreReviews = useReviews((state) => state.loadMore);

  useEffect(() => {
    ensureReviews(detail.id);
  }, [detail.id, ensureReviews]);

  const meta = TYPE_META[detail.type];

  const amenities = AMENITY_META.filter((amenity) => detail.amenities[amenity.key]);

  const links: { icon: keyof typeof Ionicons.glyphMap; label: string; url: string }[] = [];
  if (detail.phone) links.push({ icon: 'call-outline', label: 'Call', url: `tel:${detail.phone}` });
  if (detail.whatsapp) {
    links.push({
      icon: 'logo-whatsapp',
      label: 'WhatsApp',
      url: `https://wa.me/${detail.whatsapp.replace(/[^0-9]/g, '')}`,
    });
  }
  if (detail.facebook_url) {
    links.push({ icon: 'logo-facebook', label: 'Facebook', url: detail.facebook_url });
  }
  if (detail.website) {
    links.push({ icon: 'globe-outline', label: 'Website', url: detail.website });
  }

  return (
    <ScrollView
      showsVerticalScrollIndicator={false}
      contentContainerStyle={[styles.scrollContent, { paddingBottom: insets.bottom + Spacing.six }]}
    >
      <LinearGradient
        colors={[theme.accent, theme.background]}
        style={[styles.hero, { paddingTop: insets.top + 76 }]}
      >
        <Animated.View entering={FadeInDown.duration(300)} style={styles.heroContent}>
          <View style={[styles.heroBadge, { backgroundColor: theme.primary }]}>
            <Ionicons name={meta.icon} size={34} color={theme.onPrimary} />
          </View>
          <View style={styles.nameRow}>
            <ThemedText type="subtitle" style={styles.name}>
              {detail.name}
            </ThemedText>
            {detail.verified && (
              <Ionicons name="checkmark-circle" size={22} color={theme.success} />
            )}
          </View>
          <ThemedText type="small" themeColor="textSecondary" style={styles.centeredText}>
            {meta.label}
            {detail.avg_price_ar != null ? ` · ~${formatAriary(detail.avg_price_ar)}` : ''}
            {detail.district ? ` · ${detail.district}` : ''}
          </ThemedText>
          <View style={styles.heroMetaRow}>
            {detail.rating.count > 0 && detail.rating.avg_global != null && (
              <View style={styles.ratingRow}>
                <StarRow value={detail.rating.avg_global} size={14} />
                <ThemedText type="smallBold">{detail.rating.avg_global.toFixed(1)}</ThemedText>
                <ThemedText type="small" themeColor="textSecondary">
                  ({detail.rating.count})
                </ThemedText>
              </View>
            )}
            <StatusChip detail={detail} />
          </View>
        </Animated.View>
      </LinearGradient>

      <View style={styles.body}>
        {links.length > 0 && (
          <Section title="Contact" delay={60}>
            <View style={styles.chipWrap}>
              {links.map((link) => (
                <Pressable
                  key={link.label}
                  accessibilityRole="link"
                  onPress={() => void Linking.openURL(link.url).catch(() => {})}
                  style={({ pressed }) => [
                    styles.chip,
                    { backgroundColor: theme.backgroundElement, opacity: pressed ? 0.7 : 1 },
                  ]}
                >
                  <Ionicons name={link.icon} size={16} color={theme.primary} />
                  <ThemedText type="smallBold">{link.label}</ThemedText>
                </Pressable>
              ))}
            </View>
          </Section>
        )}

        <Section title="Location" delay={120}>
          <View style={styles.locationRow}>
            <Ionicons name="location-outline" size={18} color={theme.primary} />
            <View style={styles.locationText}>
              {detail.address ? <ThemedText type="small">{detail.address}</ThemedText> : null}
              <ThemedText type="small" themeColor="textSecondary">
                {[detail.district, detail.city].filter(Boolean).join(' · ')}
              </ThemedText>
            </View>
          </View>
        </Section>

        {detail.opening_hours.length > 0 && (
          <Section title="Opening hours" delay={180}>
            <OpeningHours intervals={detail.opening_hours} />
          </Section>
        )}

        {amenities.length > 0 && (
          <Section title="Good to know" delay={240}>
            <View style={styles.chipWrap}>
              {amenities.map((amenity) => (
                <View
                  key={amenity.key}
                  style={[styles.chip, { backgroundColor: theme.backgroundElement }]}
                >
                  <Ionicons name={amenity.icon} size={16} color={theme.primary} />
                  <ThemedText type="small">{amenity.label}</ThemedText>
                </View>
              ))}
            </View>
          </Section>
        )}

        {(detail.cuisines.length > 0 || detail.payment_methods.length > 0) && (
          <Section title="Cuisine & payment" delay={300}>
            <View style={styles.chipWrap}>
              {detail.cuisines.map((cuisine) => (
                <View key={cuisine.code} style={[styles.chip, { backgroundColor: theme.accent }]}>
                  <ThemedText type="small" style={{ color: theme.primary }}>
                    {cuisine.label_fr}
                  </ThemedText>
                </View>
              ))}
              {detail.payment_methods.map((method) => (
                <View
                  key={method.code}
                  style={[styles.chip, { backgroundColor: theme.backgroundElement }]}
                >
                  <Ionicons name={paymentIcon(method.code)} size={16} color={theme.primary} />
                  <ThemedText type="small">{method.label_fr}</ThemedText>
                </View>
              ))}
            </View>
          </Section>
        )}

        <Section title="Reviews" delay={360}>
          <RatingBreakdown rating={detail.rating} />
          {!reviewsEntry ||
          (reviewsEntry.status === 'loading' && reviewsEntry.items.length === 0) ? (
            <ActivityIndicator color={theme.primary} style={styles.reviewsSpinner} />
          ) : reviewsEntry.status === 'error' && reviewsEntry.items.length === 0 ? (
            <View style={styles.reviewsError}>
              <ThemedText type="small" themeColor="textSecondary">
                The reviews could not be loaded.
              </ThemedText>
              <Button
                title="Try again"
                variant="ghost"
                onPress={() => void loadReviews(detail.id)}
              />
            </View>
          ) : reviewsEntry.items.length === 0 ? (
            <ThemedText type="small" themeColor="textSecondary">
              No reviews yet — be the first to share your experience.
            </ThemedText>
          ) : (
            <View style={styles.reviewsList}>
              {reviewsEntry.items.map((review, index) => (
                <Animated.View
                  key={review.id}
                  entering={FadeInDown.duration(300).delay(Math.min(index * 60, 300))}
                >
                  <ReviewCard review={review} />
                </Animated.View>
              ))}
              {reviewsEntry.loadingMore ? (
                <ActivityIndicator color={theme.primary} style={styles.reviewsSpinner} />
              ) : reviewsEntry.nextCursor ? (
                <Button
                  title="More reviews"
                  variant="ghost"
                  onPress={() => void loadMoreReviews(detail.id)}
                />
              ) : null}
            </View>
          )}
        </Section>
      </View>
    </ScrollView>
  );
}

function StatusChip({ detail }: { detail: EstablishmentDetail }) {
  const theme = useTheme();

  const chip =
    detail.status === 'closed'
      ? { color: theme.danger, label: 'Permanently closed' }
      : detail.status === 'pending'
        ? { color: theme.textSecondary, label: 'Pending review' }
        : detail.open_now
          ? { color: theme.success, label: 'Open now' }
          : { color: theme.danger, label: 'Closed now' };

  return (
    <View style={[styles.statusChip, { backgroundColor: `${chip.color}1A` }]}>
      <View style={[styles.statusDot, { backgroundColor: chip.color }]} />
      <ThemedText type="smallBold" style={{ color: chip.color }}>
        {chip.label}
      </ThemedText>
    </View>
  );
}

function Section({
  title,
  delay = 0,
  children,
}: {
  title: string;
  delay?: number;
  children: ReactNode;
}) {
  return (
    <Animated.View entering={FadeInDown.duration(300).delay(delay)} style={styles.section}>
      <ThemedText type="smallBold" themeColor="textSecondary" style={styles.sectionTitle}>
        {title}
      </ThemedText>
      {children}
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stateBlock: {
    gap: Spacing.three,
    paddingHorizontal: Spacing.four,
    alignItems: 'stretch',
  },
  centeredText: {
    textAlign: 'center',
  },
  floatingBar: {
    position: 'absolute',
    left: Spacing.three,
    right: Spacing.three,
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  floatingButton: {
    width: 44,
    height: 44,
    borderRadius: Radius.full,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  scrollContent: {
    alignItems: 'center',
  },
  hero: {
    width: '100%',
    alignItems: 'center',
    paddingBottom: Spacing.five,
    paddingHorizontal: Spacing.four,
  },
  heroContent: {
    alignItems: 'center',
    gap: Spacing.two,
    maxWidth: MaxContentWidth,
  },
  heroBadge: {
    width: 72,
    height: 72,
    borderRadius: 22,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.one,
  },
  nameRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  name: {
    textAlign: 'center',
    flexShrink: 1,
  },
  heroMetaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    marginTop: Spacing.one,
  },
  ratingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.one,
  },
  statusChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.one,
    borderRadius: Radius.full,
    paddingHorizontal: Spacing.two + Spacing.one,
    paddingVertical: Spacing.one,
  },
  statusDot: {
    width: 8,
    height: 8,
    borderRadius: Radius.full,
  },
  body: {
    width: '100%',
    maxWidth: MaxContentWidth,
    paddingHorizontal: Spacing.four,
    gap: Spacing.five,
    paddingTop: Spacing.four,
  },
  section: {
    gap: Spacing.three,
  },
  sectionTitle: {
    textTransform: 'uppercase',
    letterSpacing: 1.2,
    fontSize: 12,
  },
  chipWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.two,
  },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.one,
    borderRadius: Radius.full,
    paddingHorizontal: Spacing.three,
    minHeight: 36,
  },
  locationRow: {
    flexDirection: 'row',
    gap: Spacing.two,
    alignItems: 'flex-start',
  },
  locationText: {
    flex: 1,
    gap: Spacing.half,
  },
  reviewsList: {
    gap: Spacing.two,
  },
  reviewsSpinner: {
    marginVertical: Spacing.two,
  },
  reviewsError: {
    gap: Spacing.two,
    alignItems: 'center',
  },
});
