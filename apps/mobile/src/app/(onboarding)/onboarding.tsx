import { useState } from 'react';

import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { Pressable, StyleSheet, useWindowDimensions, View } from 'react-native';
import Animated, {
  FadeInDown,
  interpolate,
  type SharedValue,
  useAnimatedRef,
  useAnimatedScrollHandler,
  useAnimatedStyle,
  useSharedValue,
  ZoomIn,
} from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';

import { RoundButton } from '@/components/ui/round-button';

import { Brand, BrandMark, Motif } from '@/components/brand-decor';
import { type OnboardingVisual, VisualCard } from '@/components/onboarding-visuals';
import { ThemedText } from '@/components/themed-text';

import { useOnboarding } from '@/lib/onboarding-store';

import { FontFamily, Radius, Spacing } from '@/constants/theme';

const SLIDES: {
  visual: OnboardingVisual;
  title: string;
  body: string;
  caption: string;
}[] = [
  {
    visual: 'nearby',
    title: 'Eat well, wherever you are',
    body: 'See what is cooking around you — from street vendors to restaurants, every spot earns its place on the map.',
    caption: 'Open now, within 1 km',
  },
  {
    visual: 'budget',
    title: 'Your budget leads the way',
    body: 'Filter by what you want to spend, from a 3 000 Ar plate at the corner gargotte to a full table out.',
    caption: 'The budget slider comes first',
  },
  {
    visual: 'community',
    title: 'Powered by locals',
    body: 'Rate places on five criteria, add the spots only you know about and help the map stay honest.',
    caption: 'Rated by people who eat there',
  },
];

export default function OnboardingScreen() {
  const router = useRouter();

  const complete = useOnboarding((state) => state.complete);

  const { width } = useWindowDimensions();

  const scrollRef = useAnimatedRef<Animated.ScrollView>();

  const scrollX = useSharedValue(0);

  const [page, setPage] = useState(0);
  const lastPage = SLIDES.length - 1;
  const onLastPage = page === lastPage;

  const scrollHandler = useAnimatedScrollHandler((event) => {
    scrollX.value = event.contentOffset.x;
  });

  const finish = (destination: '/sign-up' | '/sign-in') => {
    router.replace(destination);
    void complete();
  };

  return (
    <View style={styles.root}>
      <StatusBar style="light" />
      <Motif shape="ring" size={300} x={-130} y={-80} opacity={0.08} />
      <Motif shape="disc" size={120} x={290} y={120} delay={500} opacity={0.06} />
      <Motif shape="pill" size={220} x={230} y={560} rotate={40} delay={900} opacity={0.06} />
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.topBar}>
          <BrandMark size={38} />
          {!onLastPage && (
            <Pressable
              accessibilityRole="button"
              hitSlop={Spacing.two}
              onPress={() => finish('/sign-in')}
            >
              <ThemedText type="smallBold" style={styles.skip}>
                Skip
              </ThemedText>
            </Pressable>
          )}
        </View>
        <Animated.ScrollView
          ref={scrollRef}
          horizontal
          pagingEnabled
          showsHorizontalScrollIndicator={false}
          onScroll={scrollHandler}
          scrollEventThrottle={16}
          onMomentumScrollEnd={(event) =>
            setPage(Math.round(event.nativeEvent.contentOffset.x / width))
          }
        >
          {SLIDES.map((slide, index) => (
            <Slide key={slide.title} slide={slide} index={index} scrollX={scrollX} width={width} />
          ))}
        </Animated.ScrollView>
        {onLastPage && (
          <Animated.View entering={FadeInDown.duration(300)} style={styles.signInRow}>
            <Pressable
              accessibilityRole="button"
              hitSlop={Spacing.two}
              onPress={() => finish('/sign-in')}
            >
              <ThemedText type="smallBold" style={styles.signInLink}>
                I already have an account · Sign in
              </ThemedText>
            </Pressable>
          </Animated.View>
        )}
        <View style={styles.actionBar}>
          <View style={styles.dots}>
            {SLIDES.map((slide, index) => (
              <Dot key={slide.title} index={index} scrollX={scrollX} width={width} />
            ))}
          </View>
          {onLastPage ? (
            <Animated.View key="done" entering={ZoomIn.duration(250).springify().damping(15)}>
              <RoundButton
                size={52}
                backgroundColor={Brand.success}
                accessibilityLabel="Create an account"
                onPress={() => finish('/sign-up')}
              >
                <Ionicons name="checkmark" size={26} color={Brand.cream} />
              </RoundButton>
            </Animated.View>
          ) : (
            <Animated.View key="next" entering={ZoomIn.duration(250)}>
              <RoundButton
                size={52}
                backgroundColor={Brand.cream}
                accessibilityLabel="Next"
                onPress={() =>
                  scrollRef.current?.scrollTo({ x: (page + 1) * width, animated: true })
                }
              >
                <Ionicons name="chevron-forward" size={24} color={Brand.espresso} />
              </RoundButton>
            </Animated.View>
          )}
        </View>
      </SafeAreaView>
    </View>
  );
}

function Slide({
  slide,
  index,
  scrollX,
  width,
}: {
  slide: (typeof SLIDES)[number];
  index: number;
  scrollX: SharedValue<number>;
  width: number;
}) {
  const range = [(index - 1) * width, index * width, (index + 1) * width];

  const textStyle = useAnimatedStyle(() => ({
    opacity: interpolate(scrollX.value, range, [0, 1, 0]),
    transform: [{ translateX: interpolate(scrollX.value, range, [width * 0.2, 0, -width * 0.2]) }],
  }));

  const cardStyle = useAnimatedStyle(() => ({
    opacity: interpolate(scrollX.value, range, [0.2, 1, 0.2]),
    transform: [
      { translateX: interpolate(scrollX.value, range, [width * 0.45, 0, -width * 0.45]) },
      { rotate: `${interpolate(scrollX.value, range, [10, -5, -20])}deg` },
    ],
  }));

  return (
    <View style={[styles.slide, { width }]}>
      <Animated.View style={[styles.slideText, textStyle]}>
        <ThemedText style={styles.slideTitle}>{slide.title}</ThemedText>
        <ThemedText type="default" style={styles.slideBody}>
          {slide.body}
        </ThemedText>
      </Animated.View>
      <View style={styles.slideCardZone}>
        <Animated.View style={cardStyle}>
          <VisualCard visual={slide.visual} caption={slide.caption} />
        </Animated.View>
      </View>
    </View>
  );
}

function Dot({
  index,
  scrollX,
  width,
}: {
  index: number;
  scrollX: SharedValue<number>;
  width: number;
}) {
  const range = [(index - 1) * width, index * width, (index + 1) * width];

  const dotStyle = useAnimatedStyle(() => ({
    width: interpolate(scrollX.value, range, [8, 24, 8], 'clamp'),
    opacity: interpolate(scrollX.value, range, [0.4, 1, 0.4], 'clamp'),
  }));

  return <Animated.View style={[styles.dot, dotStyle]} />;
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: Brand.terracotta,
    overflow: 'hidden',
  },
  safeArea: {
    flex: 1,
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.four,
    paddingVertical: Spacing.two,
    minHeight: 52,
  },
  skip: {
    color: Brand.cream,
    opacity: 0.85,
  },
  slide: {
    paddingHorizontal: Spacing.five,
    paddingTop: Spacing.four,
    gap: Spacing.four,
  },
  slideText: {
    gap: Spacing.three,
    maxWidth: 460,
  },
  slideTitle: {
    color: Brand.cream,
    fontFamily: FontFamily.extraBold,
    fontSize: 36,
    lineHeight: 42,
  },
  slideBody: {
    color: Brand.cream,
    opacity: 0.85,
  },
  slideCardZone: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  signInRow: {
    alignItems: 'center',
    paddingBottom: Spacing.three,
  },
  signInLink: {
    color: Brand.cream,
    textDecorationLine: 'underline',
  },
  actionBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Brand.espresso,
    borderRadius: Radius.full,
    marginHorizontal: Spacing.four,
    marginBottom: Spacing.three,
    padding: Spacing.two,
    paddingLeft: Spacing.four,
    minHeight: 68,
  },
  dots: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  dot: {
    height: 8,
    borderRadius: Radius.full,
    backgroundColor: Brand.cream,
  },
});
