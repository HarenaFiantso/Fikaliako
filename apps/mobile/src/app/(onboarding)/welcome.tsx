import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { StyleSheet, View } from 'react-native';
import Animated, { FadeInDown, FadeInLeft, ZoomIn } from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';

import { RoundButton } from '@/components/ui/round-button';

import { Brand, BrandMark, Motif } from '@/components/brand-decor';
import { ThemedText } from '@/components/themed-text';

import { FontFamily, MaxContentWidth, Spacing } from '@/constants/theme';

export default function WelcomeScreen() {
  const router = useRouter();

  return (
    <View style={styles.root}>
      <StatusBar style="light" />
      <Motif shape="ring" size={340} x={180} y={-110} opacity={0.09} />
      <Motif shape="pill" size={260} x={-120} y={230} rotate={-32} delay={600} opacity={0.07} />
      <Motif shape="diamond" size={150} x={250} y={420} rotate={12} delay={1100} opacity={0.08} />
      <Motif shape="disc" size={90} x={40} y={560} delay={300} opacity={0.07} />
      <SafeAreaView style={styles.safeArea}>
        <Animated.View entering={FadeInDown.duration(400)} style={styles.topBar}>
          <BrandMark />
          <ThemedText type="smallBold" style={styles.wordmark}>
            Fikaliako
          </ThemedText>
        </Animated.View>
        <View style={styles.spacer} />
        <View style={styles.bottom}>
          <Animated.View entering={FadeInLeft.duration(450).delay(150)}>
            <ThemedText type="smallBold" style={styles.eyebrow}>
              ANTANANARIVO · STREET FOOD TO TABLES
            </ThemedText>
          </Animated.View>
          <Animated.View entering={FadeInLeft.duration(450).delay(280)}>
            <ThemedText style={styles.headline}>What am I going to eat today?</ThemedText>
          </Animated.View>
          <View style={styles.actionRow}>
            <Animated.View entering={FadeInLeft.duration(450).delay(410)} style={styles.tagline}>
              <ThemedText type="default" style={styles.taglineText}>
                Gargottes, street vendors and restaurants — found by budget, craving and distance.
              </ThemedText>
            </Animated.View>
            <Animated.View entering={ZoomIn.duration(400).delay(550).springify().damping(14)}>
              <RoundButton
                size={72}
                backgroundColor={Brand.espresso}
                accessibilityLabel="Get started"
                onPress={() => router.push('/onboarding')}
              >
                <Ionicons name="chevron-forward" size={26} color={Brand.cream} />
                <Ionicons
                  name="chevron-forward"
                  size={26}
                  color={Brand.cream}
                  style={styles.chevronOverlap}
                />
              </RoundButton>
            </Animated.View>
          </View>
        </View>
      </SafeAreaView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    alignItems: 'center',
    backgroundColor: Brand.terracotta,
    overflow: 'hidden',
  },
  safeArea: {
    flex: 1,
    width: '100%',
    maxWidth: MaxContentWidth,
    padding: Spacing.four,
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two + Spacing.one,
  },
  wordmark: {
    color: Brand.cream,
    fontSize: 18,
  },
  spacer: {
    flex: 1,
  },
  bottom: {
    gap: Spacing.three,
  },
  eyebrow: {
    color: Brand.cream,
    opacity: 0.75,
    fontSize: 12,
    letterSpacing: 2,
  },
  headline: {
    color: Brand.cream,
    fontFamily: FontFamily.extraBold,
    fontSize: 46,
    lineHeight: 52,
  },
  actionRow: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    justifyContent: 'space-between',
    gap: Spacing.four,
    marginTop: Spacing.two,
  },
  tagline: {
    flex: 1,
    maxWidth: 420,
  },
  taglineText: {
    color: Brand.cream,
    opacity: 0.85,
  },
  chevronOverlap: {
    marginLeft: -18,
  },
});
