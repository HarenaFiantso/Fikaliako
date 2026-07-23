import { type ReactNode, useEffect } from 'react';

import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, View } from 'react-native';
import Animated, {
  Easing,
  useAnimatedStyle,
  useSharedValue,
  withRepeat,
  withTiming,
} from 'react-native-reanimated';

import { Brand } from '@/components/brand-decor';
import { ThemedText } from '@/components/themed-text';

import { FontFamily, Radius, Spacing } from '@/constants/theme';

export type OnboardingVisual = 'nearby' | 'budget' | 'community';

const CARD_SIZE = 250;

/** The floating cream card each onboarding slide carries, reference-style tilt included. */
export function VisualCard({ visual, caption }: { visual: OnboardingVisual; caption: string }) {
  return (
    <View style={styles.card}>
      <View style={styles.cardBody}>
        {visual === 'nearby' && <NearbyRadar />}
        {visual === 'budget' && <BudgetLadder />}
        {visual === 'community' && <CriteriaOrbit />}
      </View>
      <ThemedText type="smallBold" style={styles.caption}>
        {caption}
      </ThemedText>
    </View>
  );
}

/** A radar view: you in the middle, places to eat inside the 1 km ring. */
function NearbyRadar() {
  return (
    <View style={styles.stage}>
      <View style={[styles.radarRing, styles.radarOuter]} />
      <View style={[styles.radarRing, styles.radarInner]} />
      <View style={styles.radarCenter}>
        <Ionicons name="navigate" size={24} color={Brand.cream} />
      </View>
      <FoodDot icon="fast-food-outline" x={16} y={28} />
      <FoodDot icon="restaurant-outline" x={138} y={10} />
      <FoodDot icon="cafe-outline" x={150} y={128} />
      <FoodDot icon="pizza-outline" x={6} y={122} />
    </View>
  );
}

function FoodDot({
  icon,
  x,
  y,
}: {
  icon: 'fast-food-outline' | 'restaurant-outline' | 'cafe-outline' | 'pizza-outline';
  x: number;
  y: number;
}) {
  return (
    <View style={[styles.foodDot, { left: x, top: y }]}>
      <Ionicons name={icon} size={16} color={Brand.terracotta} />
    </View>
  );
}

const TRANCHES = [
  { label: '0 – 3 000 Ar', width: 0.45 },
  { label: '3 – 5 000 Ar', width: 0.6, selected: true },
  { label: '5 – 10 000 Ar', width: 0.78 },
  { label: '10 000+ Ar', width: 0.95 },
] as const;

/** The four budget tranches, the king filter, drawn as a pill ladder. */
function BudgetLadder() {
  return (
    <View style={styles.ladder}>
      {TRANCHES.map((tranche) => (
        <View
          key={tranche.label}
          style={[
            styles.tranche,
            { width: `${tranche.width * 100}%` },
            'selected' in tranche && styles.trancheSelected,
          ]}
        >
          <ThemedText
            type="smallBold"
            style={'selected' in tranche ? styles.trancheSelectedText : styles.trancheText}
          >
            {tranche.label}
          </ThemedText>
          {'selected' in tranche && <Ionicons name="checkmark" size={16} color={Brand.cream} />}
        </View>
      ))}
    </View>
  );
}

const CRITERIA = [
  'sparkles-outline',
  'pricetag-outline',
  'water-outline',
  'flash-outline',
  'happy-outline',
] as const;

const ORBIT_SIZE = 190;
const ORBIT_RADIUS = 78;
const SATELLITE_SIZE = 40;

/** The five review criteria orbiting the community star, slowly spinning. */
function CriteriaOrbit() {
  const spin = useSharedValue(0);

  useEffect(() => {
    spin.value = withRepeat(withTiming(360, { duration: 30000, easing: Easing.linear }), -1);
  }, [spin]);

  const orbitStyle = useAnimatedStyle(() => ({
    transform: [{ rotate: `${spin.value}deg` }],
  }));
  const counterStyle = useAnimatedStyle(() => ({
    transform: [{ rotate: `${-spin.value}deg` }],
  }));

  return (
    <View style={styles.orbitStage}>
      <View style={styles.orbitRing} />
      <View style={styles.orbitCenter}>
        <Ionicons name="star" size={26} color={Brand.cream} />
      </View>
      <Animated.View style={[StyleSheet.absoluteFill, orbitStyle]}>
        {CRITERIA.map((icon, index) => {
          const angle = (index / CRITERIA.length) * 2 * Math.PI - Math.PI / 2;
          const left = ORBIT_SIZE / 2 + ORBIT_RADIUS * Math.cos(angle) - SATELLITE_SIZE / 2;
          const top = ORBIT_SIZE / 2 + ORBIT_RADIUS * Math.sin(angle) - SATELLITE_SIZE / 2;
          return (
            <Animated.View key={icon} style={[styles.satellite, { left, top }, counterStyle]}>
              <Ionicons name={icon} size={18} color={Brand.terracotta} />
            </Animated.View>
          );
        })}
      </Animated.View>
    </View>
  );
}

export function TiltedWrapper({ children }: { children: ReactNode }) {
  return <View style={styles.tilt}>{children}</View>;
}

const styles = StyleSheet.create({
  tilt: {
    transform: [{ rotate: '-5deg' }],
  },
  card: {
    width: CARD_SIZE + Spacing.four * 2,
    borderRadius: Radius.xxl + 8,
    backgroundColor: Brand.cream,
    padding: Spacing.four,
    gap: Spacing.three,
    shadowColor: '#000',
    shadowOpacity: 0.22,
    shadowRadius: 28,
    shadowOffset: { width: 0, height: 14 },
    elevation: 10,
  },
  cardBody: {
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: ORBIT_SIZE + Spacing.two,
  },
  caption: {
    color: Brand.espresso,
    fontFamily: FontFamily.bold,
  },
  stage: {
    width: ORBIT_SIZE,
    height: ORBIT_SIZE,
    alignItems: 'center',
    justifyContent: 'center',
  },
  radarRing: {
    position: 'absolute',
    borderColor: Brand.terracotta,
    borderRadius: Radius.full,
  },
  radarOuter: {
    width: ORBIT_SIZE,
    height: ORBIT_SIZE,
    borderWidth: 1.5,
    borderStyle: 'dashed',
    opacity: 0.5,
  },
  radarInner: {
    width: ORBIT_SIZE * 0.58,
    height: ORBIT_SIZE * 0.58,
    borderWidth: 1.5,
    opacity: 0.25,
  },
  radarCenter: {
    width: 52,
    height: 52,
    borderRadius: Radius.full,
    backgroundColor: Brand.terracotta,
    alignItems: 'center',
    justifyContent: 'center',
  },
  foodDot: {
    position: 'absolute',
    width: 36,
    height: 36,
    borderRadius: Radius.full,
    backgroundColor: Brand.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  ladder: {
    width: '100%',
    gap: Spacing.two + Spacing.one,
  },
  tranche: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Brand.accent,
    borderRadius: Radius.full,
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two + Spacing.one,
  },
  trancheSelected: {
    backgroundColor: Brand.terracotta,
  },
  trancheText: {
    color: Brand.espresso,
  },
  trancheSelectedText: {
    color: Brand.cream,
  },
  orbitStage: {
    width: ORBIT_SIZE,
    height: ORBIT_SIZE,
    alignItems: 'center',
    justifyContent: 'center',
  },
  orbitRing: {
    position: 'absolute',
    width: ORBIT_RADIUS * 2,
    height: ORBIT_RADIUS * 2,
    borderRadius: Radius.full,
    borderWidth: 1.5,
    borderStyle: 'dashed',
    borderColor: Brand.terracotta,
    opacity: 0.4,
  },
  orbitCenter: {
    width: 56,
    height: 56,
    borderRadius: Radius.full,
    backgroundColor: Brand.terracotta,
    alignItems: 'center',
    justifyContent: 'center',
  },
  satellite: {
    position: 'absolute',
    width: SATELLITE_SIZE,
    height: SATELLITE_SIZE,
    borderRadius: Radius.full,
    backgroundColor: Brand.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
