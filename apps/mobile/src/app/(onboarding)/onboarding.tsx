import { useState } from 'react';

import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { StyleSheet, useWindowDimensions, View } from 'react-native';
import Animated, {
  FadeInDown,
  interpolate,
  type SharedValue,
  useAnimatedRef,
  useAnimatedScrollHandler,
  useAnimatedStyle,
  useSharedValue,
} from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/button';

import { ThemedText } from '@/components/themed-text';

import { useTheme } from '@/hooks/use-theme';

import { useOnboarding } from '@/lib/onboarding-store';

import { Radius, Spacing } from '@/constants/theme';

const SLIDES = [
  {
    icon: 'location-outline',
    title: 'Eat well, wherever you are',
    body: 'See what is cooking around you — from street vendors to restaurants, every spot earns its place on the map.',
  },
  {
    icon: 'wallet-outline',
    title: 'Your budget leads the way',
    body: 'Filter by what you want to spend, from a 3 000 Ar plate at the corner gargotte to a full table out.',
  },
  {
    icon: 'people-outline',
    title: 'Powered by locals',
    body: 'Rate places on five criteria, add the spots only you know about and help the map stay honest.',
  },
] as const;

export default function OnboardingScreen() {
  const router = useRouter();
  const theme = useTheme();
  const complete = useOnboarding((state) => state.complete);
  const { width } = useWindowDimensions();

  const scrollRef = useAnimatedRef<Animated.ScrollView>();
  const scrollX = useSharedValue(0);
  const [page, setPage] = useState(0);
  const lastPage = SLIDES.length - 1;

  const scrollHandler = useAnimatedScrollHandler((event) => {
    scrollX.value = event.contentOffset.x;
  });

  const finish = (destination: 'sign-up' | 'sign-in' | 'browse') => {
    void complete();
    router.replace(destination === 'browse' ? '/' : '/profile');
    if (destination !== 'browse') {
      router.push(destination === 'sign-up' ? '/sign-up' : '/sign-in');
    }
  };

  return (
    <View style={[styles.root, { backgroundColor: theme.background }]}>
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.topBar}>
          {page < lastPage && (
            <Button title="Skip" variant="ghost" onPress={() => finish('browse')} />
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

        <View style={styles.footer}>
          <View style={styles.dots}>
            {SLIDES.map((slide, index) => (
              <Dot key={slide.title} index={index} scrollX={scrollX} width={width} />
            ))}
          </View>

          {page < lastPage ? (
            <Button
              title="Next"
              onPress={() => scrollRef.current?.scrollTo({ x: (page + 1) * width, animated: true })}
            />
          ) : (
            <Animated.View entering={FadeInDown.duration(300)} style={styles.finalActions}>
              <Button title="Create an account" onPress={() => finish('sign-up')} />
              <Button
                title="I already have one"
                variant="secondary"
                onPress={() => finish('sign-in')}
              />
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
  const theme = useTheme();
  const range = [(index - 1) * width, index * width, (index + 1) * width];

  const iconStyle = useAnimatedStyle(() => ({
    opacity: interpolate(scrollX.value, range, [0, 1, 0]),
    transform: [
      { translateX: interpolate(scrollX.value, range, [width * 0.35, 0, -width * 0.35]) },
      { scale: interpolate(scrollX.value, range, [0.6, 1, 0.6]) },
    ],
  }));

  const textStyle = useAnimatedStyle(() => ({
    opacity: interpolate(scrollX.value, range, [0, 1, 0]),
    transform: [
      { translateX: interpolate(scrollX.value, range, [width * 0.18, 0, -width * 0.18]) },
    ],
  }));

  return (
    <View style={[styles.slide, { width }]}>
      <Animated.View
        style={[styles.slideIcon, { backgroundColor: theme.backgroundSelected }, iconStyle]}
      >
        <Ionicons name={slide.icon} size={64} color={theme.primary} />
      </Animated.View>
      <Animated.View style={[styles.slideText, textStyle]}>
        <ThemedText type="subtitle" style={styles.centered}>
          {slide.title}
        </ThemedText>
        <ThemedText type="default" themeColor="textSecondary" style={styles.centered}>
          {slide.body}
        </ThemedText>
      </Animated.View>
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
  const theme = useTheme();
  const range = [(index - 1) * width, index * width, (index + 1) * width];

  const dotStyle = useAnimatedStyle(() => ({
    width: interpolate(scrollX.value, range, [8, 24, 8], 'clamp'),
    opacity: interpolate(scrollX.value, range, [0.35, 1, 0.35], 'clamp'),
  }));

  return <Animated.View style={[styles.dot, { backgroundColor: theme.primary }, dotStyle]} />;
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
  safeArea: {
    flex: 1,
  },
  topBar: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    paddingHorizontal: Spacing.three,
    minHeight: 52,
  },
  slide: {
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.five,
    paddingHorizontal: Spacing.five,
  },
  slideIcon: {
    width: 160,
    height: 160,
    borderRadius: Radius.full,
    alignItems: 'center',
    justifyContent: 'center',
  },
  slideText: {
    gap: Spacing.three,
    maxWidth: 420,
  },
  centered: {
    textAlign: 'center',
  },
  footer: {
    padding: Spacing.four,
    gap: Spacing.four,
  },
  dots: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    gap: Spacing.two,
  },
  dot: {
    height: 8,
    borderRadius: Radius.full,
  },
  finalActions: {
    gap: Spacing.two,
  },
});
