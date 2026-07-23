import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, View } from 'react-native';

import type { EstablishmentSummary, EstablishmentType } from '@fikaliako/api-client';

import { FavoriteButton } from '@/components/favorite-button';
import { ThemedText } from '@/components/themed-text';

import { useTheme } from '@/hooks/use-theme';

import { formatAriary } from '@/lib/format';

import { FontFamily, Radius, Spacing } from '@/constants/theme';

const TYPE_META: Record<
  EstablishmentType,
  { label: string; icon: keyof typeof Ionicons.glyphMap }
> = {
  restaurant: { label: 'Restaurant', icon: 'restaurant-outline' },
  gargotte: { label: 'Gargotte', icon: 'storefront-outline' },
  cafe: { label: 'Café', icon: 'cafe-outline' },
  snack: { label: 'Snack', icon: 'fast-food-outline' },
  food_truck: { label: 'Food truck', icon: 'car-outline' },
  street_vendor: { label: 'Street vendor', icon: 'basket-outline' },
  pastry_shop: { label: 'Pastry shop', icon: 'ice-cream-outline' },
  bar_restaurant: { label: 'Bar-restaurant', icon: 'wine-outline' },
  hotel_restaurant: { label: 'Hotel restaurant', icon: 'bed-outline' },
};

export function EstablishmentCard({ establishment }: { establishment: EstablishmentSummary }) {
  const theme = useTheme();

  const meta = TYPE_META[establishment.type];
  const priceLabel =
    establishment.avg_price_ar != null ? ` · ~${formatAriary(establishment.avg_price_ar)}` : '';

  return (
    <View style={[styles.card, { backgroundColor: theme.card, borderColor: theme.border }]}>
      <View style={[styles.typeBadge, { backgroundColor: theme.accent }]}>
        <Ionicons name={meta.icon} size={22} color={theme.primary} />
      </View>
      <View style={styles.info}>
        <View style={styles.nameRow}>
          <ThemedText type="default" style={styles.name} numberOfLines={1}>
            {establishment.name}
          </ThemedText>
          {establishment.verified && (
            <Ionicons name="checkmark-circle" size={16} color={theme.success} />
          )}
        </View>
        <ThemedText type="small" themeColor="textSecondary" numberOfLines={1}>
          {meta.label}
          {priceLabel}
        </ThemedText>
        {establishment.rating_avg != null && establishment.rating_count > 0 ? (
          <View style={styles.ratingRow}>
            <Ionicons name="star" size={13} color={theme.primary} />
            <ThemedText type="smallBold">{establishment.rating_avg.toFixed(1)}</ThemedText>
            <ThemedText type="small" themeColor="textSecondary">
              ({establishment.rating_count})
            </ThemedText>
          </View>
        ) : (
          <ThemedText type="small" themeColor="textSecondary">
            No reviews yet
          </ThemedText>
        )}
      </View>
      <FavoriteButton establishment={establishment} />
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    borderWidth: 1,
    borderRadius: Radius.xxl,
    padding: Spacing.three,
  },
  typeBadge: {
    width: 48,
    height: 48,
    borderRadius: Radius.xl,
    alignItems: 'center',
    justifyContent: 'center',
  },
  info: {
    flex: 1,
    gap: Spacing.half,
  },
  nameRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.one,
  },
  name: {
    fontFamily: FontFamily.bold,
    flexShrink: 1,
  },
  ratingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.one,
  },
});
