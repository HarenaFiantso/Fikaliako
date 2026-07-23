import { useEffect, useState } from 'react';

import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { ActivityIndicator, Pressable, RefreshControl, StyleSheet, View } from 'react-native';
import Animated, {
  Easing,
  FadeInDown,
  FadeOut,
  LinearTransition,
  useAnimatedStyle,
  useSharedValue,
  withRepeat,
  withTiming,
} from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';

import type { EstablishmentSummary } from '@fikaliako/api-client';

import { Button } from '@/components/ui/button';

import { EstablishmentCard } from '@/components/establishment-card';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';

import { useTheme } from '@/hooks/use-theme';

import { useFavorites } from '@/lib/favorites-store';

import { MaxContentWidth, Radius, Spacing } from '@/constants/theme';

export default function FavoritesScreen() {
  const router = useRouter();

  const theme = useTheme();

  const status = useFavorites((state) => state.status);
  const items = useFavorites((state) => state.items);
  const loadingMore = useFavorites((state) => state.loadingMore);
  const load = useFavorites((state) => state.load);
  const ensureLoaded = useFavorites((state) => state.ensureLoaded);
  const loadMore = useFavorites((state) => state.loadMore);

  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    ensureLoaded();
  }, [ensureLoaded]);

  const onRefresh = async () => {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  };

  return (
    <ThemedView style={styles.root}>
      <SafeAreaView style={styles.safeArea}>
        <Animated.View entering={FadeInDown.duration(300)} style={styles.header}>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Go back"
            onPress={() => router.back()}
            style={[styles.backButton, { backgroundColor: theme.card, borderColor: theme.border }]}
          >
            <Ionicons name="chevron-back" size={22} color={theme.text} />
          </Pressable>
          <View style={styles.headerText}>
            <ThemedText type="subtitle">Favorites</ThemedText>
            {status === 'ready' && items.length > 0 && (
              <ThemedText type="small" themeColor="textSecondary">
                {items.length} place{items.length > 1 ? 's' : ''} you love
              </ThemedText>
            )}
          </View>
        </Animated.View>

        {status === 'loading' && items.length === 0 ? (
          <View style={styles.centered}>
            <ActivityIndicator color={theme.primary} />
          </View>
        ) : status === 'error' ? (
          <View style={[styles.centered, styles.stateBlock]}>
            <ThemedText type="default" themeColor="textSecondary" style={styles.centeredText}>
              Your favorites could not be loaded.
            </ThemedText>
            <Button title="Try again" variant="secondary" onPress={() => void load()} />
          </View>
        ) : items.length === 0 ? (
          <EmptyFavorites onExplore={() => router.push('/explore')} />
        ) : (
          <Animated.FlatList
            data={items}
            keyExtractor={(item: EstablishmentSummary) => item.id}
            renderItem={({ item, index }) => (
              <Animated.View
                entering={FadeInDown.duration(300).delay(Math.min(index * 60, 360))}
                exiting={FadeOut.duration(180)}
              >
                <EstablishmentCard establishment={item} />
              </Animated.View>
            )}
            itemLayoutAnimation={LinearTransition.springify().damping(18)}
            contentContainerStyle={styles.listContent}
            showsVerticalScrollIndicator={false}
            onEndReached={() => void loadMore()}
            onEndReachedThreshold={0.4}
            refreshControl={
              <RefreshControl
                refreshing={refreshing}
                onRefresh={() => void onRefresh()}
                tintColor={theme.primary}
              />
            }
            ListFooterComponent={
              loadingMore ? (
                <ActivityIndicator color={theme.primary} style={styles.footerSpinner} />
              ) : null
            }
          />
        )}
      </SafeAreaView>
    </ThemedView>
  );
}

function EmptyFavorites({ onExplore }: { onExplore: () => void }) {
  const theme = useTheme();

  const pulse = useSharedValue(0);

  useEffect(() => {
    pulse.value = withRepeat(
      withTiming(1, { duration: 1400, easing: Easing.inOut(Easing.sin) }),
      -1,
      true
    );
  }, [pulse]);

  const heartStyle = useAnimatedStyle(() => ({
    transform: [{ scale: 1 + pulse.value * 0.08 }],
  }));

  return (
    <Animated.View entering={FadeInDown.duration(300).delay(80)} style={styles.empty}>
      <Animated.View style={[styles.emptyHeart, { backgroundColor: theme.accent }, heartStyle]}>
        <Ionicons name="heart" size={40} color={theme.primary} />
      </Animated.View>
      <ThemedText type="subtitle" style={styles.centeredText}>
        No favorites yet
      </ThemedText>
      <ThemedText type="default" themeColor="textSecondary" style={styles.centeredText}>
        Tap the heart on a place you love and it will wait for you here.
      </ThemedText>
      <Button title="Explore places" onPress={onExplore} />
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    alignItems: 'center',
  },
  safeArea: {
    flex: 1,
    width: '100%',
    maxWidth: MaxContentWidth,
    paddingHorizontal: Spacing.four,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    paddingVertical: Spacing.three,
  },
  headerText: {
    gap: Spacing.half,
  },
  backButton: {
    width: 44,
    height: 44,
    borderRadius: Radius.full,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stateBlock: {
    gap: Spacing.three,
    paddingHorizontal: Spacing.four,
  },
  centeredText: {
    textAlign: 'center',
  },
  listContent: {
    gap: Spacing.two,
    paddingBottom: Spacing.five,
  },
  footerSpinner: {
    marginVertical: Spacing.three,
  },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.three,
    paddingHorizontal: Spacing.four,
    paddingBottom: Spacing.six,
  },
  emptyHeart: {
    width: 96,
    height: 96,
    borderRadius: Radius.full,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.two,
  },
});
