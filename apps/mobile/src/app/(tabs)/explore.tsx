import { useEffect, useState } from 'react';

import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { ActivityIndicator, Pressable, RefreshControl, StyleSheet, View } from 'react-native';
import Animated, { FadeInDown } from 'react-native-reanimated';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';

import type { EstablishmentSummary } from '@fikaliako/api-client';

import { Button } from '@/components/ui/button';

import { BrandMark } from '@/components/brand-decor';
import { EstablishmentCard } from '@/components/establishment-card';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';

import { useTheme } from '@/hooks/use-theme';

import { useEstablishmentBrowse } from '@/lib/browse-store';
import { useFavorites } from '@/lib/favorites-store';

import { BottomTabInset, MaxContentWidth, Radius, Spacing } from '@/constants/theme';

export default function ExploreScreen() {
  const router = useRouter();
  const theme = useTheme();
  const insets = useSafeAreaInsets();

  const ensureFavorites = useFavorites((state) => state.ensureLoaded);

  const status = useEstablishmentBrowse((state) => state.status);
  const items = useEstablishmentBrowse((state) => state.items);
  const loadingMore = useEstablishmentBrowse((state) => state.loadingMore);
  const load = useEstablishmentBrowse((state) => state.load);
  const ensureLoaded = useEstablishmentBrowse((state) => state.ensureLoaded);
  const loadMore = useEstablishmentBrowse((state) => state.loadMore);

  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    ensureLoaded();
    ensureFavorites();
  }, [ensureLoaded, ensureFavorites]);

  const onRefresh = async () => {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  };

  return (
    <ThemedView style={styles.root}>
      <SafeAreaView style={styles.safeArea} edges={['top', 'left', 'right']}>
        <Animated.View entering={FadeInDown.duration(300)} style={styles.header}>
          <View style={styles.headerTitleRow}>
            <BrandMark size={36} tone="terracotta" />
            <ThemedText type="subtitle">Explore</ThemedText>
            <View style={styles.headerSpacer} />
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Add a place"
              onPress={() => router.push('/propose')}
              style={({ pressed }) => [
                styles.addButton,
                { backgroundColor: theme.accent, opacity: pressed ? 0.7 : 1 },
              ]}
            >
              <Ionicons name="add" size={24} color={theme.primary} />
            </Pressable>
          </View>
          <ThemedText type="small" themeColor="textSecondary">
            Aiza no hisakafo androany ?
          </ThemedText>
        </Animated.View>

        {status === 'loading' && items.length === 0 ? (
          <View style={styles.centered}>
            <ActivityIndicator color={theme.primary} />
          </View>
        ) : status === 'error' && items.length === 0 ? (
          <View style={[styles.centered, styles.stateBlock]}>
            <ThemedText type="default" themeColor="textSecondary" style={styles.centeredText}>
              The places around could not be loaded.
            </ThemedText>
            <Button title="Try again" variant="secondary" onPress={() => void load()} />
          </View>
        ) : items.length === 0 ? (
          <View style={[styles.centered, styles.stateBlock]}>
            <ThemedText type="default" themeColor="textSecondary" style={styles.centeredText}>
              No establishments yet — the community is just getting started.
            </ThemedText>
          </View>
        ) : (
          <Animated.FlatList
            data={items}
            keyExtractor={(item: EstablishmentSummary) => item.id}
            renderItem={({ item, index }) => (
              <Animated.View entering={FadeInDown.duration(300).delay(Math.min(index * 60, 360))}>
                <EstablishmentCard
                  establishment={item}
                  onPress={() =>
                    router.push({
                      pathname: '/establishment/[idOrSlug]',
                      params: { idOrSlug: item.slug },
                    })
                  }
                />
              </Animated.View>
            )}
            contentContainerStyle={[
              styles.listContent,
              { paddingBottom: insets.bottom + BottomTabInset },
            ]}
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

const styles = StyleSheet.create({
  root: {
    flex: 1,
    alignItems: 'center',
    overflow: 'hidden',
  },
  safeArea: {
    flex: 1,
    width: '100%',
    maxWidth: MaxContentWidth,
    paddingHorizontal: Spacing.four,
  },
  header: {
    gap: Spacing.one,
    paddingTop: Spacing.three,
    paddingBottom: Spacing.three,
  },
  headerTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  headerSpacer: {
    flex: 1,
  },
  addButton: {
    width: 44,
    height: 44,
    borderRadius: Radius.full,
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
  },
  footerSpinner: {
    marginVertical: Spacing.three,
  },
});
