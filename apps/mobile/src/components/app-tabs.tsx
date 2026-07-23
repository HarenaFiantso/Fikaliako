import { useEffect, useState } from 'react';

import { Ionicons } from '@expo/vector-icons';
import { GlassView, isLiquidGlassAvailable } from 'expo-glass-effect';
import { usePathname } from 'expo-router';
import {
  TabList,
  type TabListProps,
  Tabs,
  TabSlot,
  TabTrigger,
  type TabTriggerSlotProps,
} from 'expo-router/ui';
import { Platform, Pressable, StyleSheet, View } from 'react-native';
import Animated, {
  interpolate,
  useAnimatedStyle,
  useSharedValue,
  withSpring,
} from 'react-native-reanimated';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ThemedText } from '@/components/themed-text';

import { useTheme } from '@/hooks/use-theme';

import { FontFamily, Radius, Spacing } from '@/constants/theme';

const TABS = [
  { name: 'index', href: '/', label: 'Home', icon: 'home', iconIdle: 'home-outline' },
  {
    name: 'explore',
    href: '/explore',
    label: 'Explore',
    icon: 'compass',
    iconIdle: 'compass-outline',
  },
  {
    name: 'profile',
    href: '/profile',
    label: 'Profile',
    icon: 'person',
    iconIdle: 'person-outline',
  },
] as const;

const BAR_HEIGHT = 64;
const BAR_PADDING = 6;

export default function AppTabs() {
  return (
    <Tabs>
      <TabSlot />
      <TabList asChild>
        <GlassTabBar>
          {TABS.map((tab) => (
            <TabTrigger key={tab.name} name={tab.name} href={tab.href} asChild>
              <TabButton label={tab.label} icon={tab.icon} iconIdle={tab.iconIdle} />
            </TabTrigger>
          ))}
        </GlassTabBar>
      </TabList>
    </Tabs>
  );
}

function GlassTabBar({ children, ...props }: TabListProps) {
  const theme = useTheme();
  const insets = useSafeAreaInsets();

  const [barWidth, setBarWidth] = useState(0);
  const segment = barWidth > 0 ? (barWidth - BAR_PADDING * 2) / TABS.length : 0;

  const pathname = usePathname();
  const index = Math.max(
    0,
    TABS.findIndex((tab) => tab.href === pathname)
  );

  const position = useSharedValue(index);

  useEffect(() => {
    position.value = withSpring(index, { damping: 14, stiffness: 180 });
  }, [index, position]);

  /**
   * The pill trails the active tab with an underdamped spring; the distance
   * to the nearest tab drives a horizontal stretch and vertical squash, so
   * mid-flight the pill deforms like a droplet and settles round.
   */
  const indicatorStyle = useAnimatedStyle(() => {
    const wobble = Math.abs(position.value - Math.round(position.value));
    return {
      transform: [
        { translateX: position.value * segment },
        { scaleX: 1 + wobble * 0.35 },
        { scaleY: 1 - wobble * 0.12 },
      ],
    };
  });

  const liquidGlass = Platform.OS === 'ios' && isLiquidGlassAvailable();

  const barContent = (
    <>
      {segment > 0 && (
        <Animated.View
          pointerEvents="none"
          style={[
            styles.indicator,
            {
              width: segment,
              backgroundColor: liquidGlass ? `${theme.accent}D9` : theme.accent,
            },
            indicatorStyle,
          ]}
        />
      )}
      {children}
    </>
  );

  const onBarLayout = ({ nativeEvent }: { nativeEvent: { layout: { width: number } } }) =>
    setBarWidth(nativeEvent.layout.width);

  return (
    <View
      {...props}
      pointerEvents="box-none"
      style={[styles.wrap, { bottom: insets.bottom + Spacing.two }]}
    >
      {liquidGlass ? (
        <GlassView glassEffectStyle="clear" style={styles.bar} onLayout={onBarLayout}>
          {barContent}
        </GlassView>
      ) : (
        <View
          style={[
            styles.bar,
            styles.barFallback,
            { backgroundColor: `${theme.card}F0`, borderColor: theme.border },
          ]}
          onLayout={onBarLayout}
        >
          {barContent}
        </View>
      )}
    </View>
  );
}

type TabButtonProps = TabTriggerSlotProps & {
  label: string;
  icon: keyof typeof Ionicons.glyphMap;
  iconIdle: keyof typeof Ionicons.glyphMap;
};

function TabButton({ label, icon, iconIdle, isFocused, ...props }: TabButtonProps) {
  const theme = useTheme();

  const focus = useSharedValue(isFocused ? 1 : 0);

  useEffect(() => {
    focus.value = withSpring(isFocused ? 1 : 0, { damping: 15, stiffness: 260 });
  }, [focus, isFocused]);

  const iconStyle = useAnimatedStyle(() => ({
    transform: [
      { scale: interpolate(focus.value, [0, 1], [1, 1.12]) },
      { translateY: interpolate(focus.value, [0, 1], [0, -1.5]) },
    ],
  }));

  const color = isFocused ? theme.primary : theme.textSecondary;

  return (
    <Pressable {...props} accessibilityRole="button" accessibilityLabel={label} style={styles.tab}>
      <Animated.View style={iconStyle}>
        <Ionicons name={isFocused ? icon : iconIdle} size={22} color={color} />
      </Animated.View>
      <ThemedText style={[styles.tabLabel, { color }]}>{label}</ThemedText>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  wrap: {
    position: 'absolute',
    left: 0,
    right: 0,
    alignItems: 'center',
    paddingHorizontal: Spacing.four,
  },
  bar: {
    flexDirection: 'row',
    alignItems: 'center',
    width: '100%',
    maxWidth: 420,
    height: BAR_HEIGHT,
    borderRadius: BAR_HEIGHT / 2,
    paddingHorizontal: BAR_PADDING,
    overflow: 'hidden',
  },
  barFallback: {
    borderWidth: 1,
    shadowColor: '#000',
    shadowOpacity: 0.12,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 6 },
    elevation: 8,
  },
  indicator: {
    position: 'absolute',
    left: BAR_PADDING,
    top: BAR_PADDING,
    bottom: BAR_PADDING,
    borderRadius: (BAR_HEIGHT - BAR_PADDING * 2) / 2,
  },
  tab: {
    flex: 1,
    height: '100%',
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.half,
    borderRadius: Radius.full,
  },
  tabLabel: {
    fontFamily: FontFamily.semiBold,
    fontSize: 11,
    lineHeight: 14,
  },
});
