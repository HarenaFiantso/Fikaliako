import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { Alert, Platform, Pressable, StyleSheet, View } from 'react-native';
import Animated, { FadeInDown } from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/button';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';

import { useTheme } from '@/hooks/use-theme';

import { formatPhone } from '@/lib/auth/phone';
import { useSession } from '@/lib/auth/session-store';

import { BottomTabInset, MaxContentWidth, Radius, Spacing } from '@/constants/theme';

export default function ProfileScreen() {
  const router = useRouter();

  const theme = useTheme();

  const user = useSession((state) => state.user);
  const signOut = useSession((state) => state.signOut);

  if (!user) return null;

  const initials = user.display_name
    .split(/\s+/)
    .slice(0, 2)
    .map((word) => word[0]?.toUpperCase() ?? '')
    .join('');

  const memberSince = new Date(user.created_at).toLocaleDateString('en', {
    month: 'long',
    year: 'numeric',
  });

  const confirmSignOut = () => {
    if (Platform.OS === 'web') {
      void signOut();
      return;
    }
    Alert.alert('Sign out', 'You can sign back in at any time.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Sign out', style: 'destructive', onPress: () => void signOut() },
    ]);
  };

  return (
    <ThemedView style={styles.root}>
      <SafeAreaView style={styles.safeArea}>
        <Animated.View entering={FadeInDown.duration(300)} style={styles.identity}>
          <View style={[styles.avatar, { backgroundColor: theme.primary }]}>
            <ThemedText type="subtitle" style={{ color: theme.onPrimary }}>
              {initials}
            </ThemedText>
          </View>
          <ThemedText type="subtitle">{user.display_name}</ThemedText>
          <View style={styles.identityRow}>
            <ThemedText type="small" themeColor="textSecondary">
              {formatPhone(user.phone)}
            </ThemedText>
            {user.phone_verified && (
              <View style={styles.identityRow}>
                <Ionicons name="checkmark-circle" size={16} color={theme.success} />
                <ThemedText type="small" style={{ color: theme.success }}>
                  Verified
                </ThemedText>
              </View>
            )}
          </View>
          <ThemedText type="small" themeColor="textSecondary">
            {user.role === 'business' ? 'Business account' : 'Member'} since {memberSince}
          </ThemedText>
        </Animated.View>
        <Animated.View entering={FadeInDown.duration(300).delay(80)} style={styles.menu}>
          <MenuRow
            icon="key-outline"
            label="Change password"
            onPress={() => router.push('/change-password')}
          />
        </Animated.View>
        <Animated.View entering={FadeInDown.duration(300).delay(160)}>
          <Button title="Sign out" variant="danger" onPress={confirmSignOut} />
        </Animated.View>
      </SafeAreaView>
    </ThemedView>
  );
}

function MenuRow({
  icon,
  label,
  onPress,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  onPress: () => void;
}) {
  const theme = useTheme();

  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [
        styles.menuRow,
        { backgroundColor: theme.backgroundElement, opacity: pressed ? 0.7 : 1 },
      ]}
    >
      <Ionicons name={icon} size={20} color={theme.textSecondary} />
      <ThemedText type="default" style={styles.menuLabel}>
        {label}
      </ThemedText>
      <Ionicons name="chevron-forward" size={18} color={theme.textSecondary} />
    </Pressable>
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
    padding: Spacing.four,
    paddingBottom: BottomTabInset + Spacing.three,
    gap: Spacing.five,
  },
  identity: {
    alignItems: 'center',
    gap: Spacing.two,
    paddingTop: Spacing.six,
  },
  avatar: {
    width: 72,
    height: 72,
    borderRadius: Radius.full,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.two,
  },
  identityRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.one,
  },
  menu: {
    flex: 1,
    gap: Spacing.two,
  },
  menuRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    borderRadius: Radius.xl,
    paddingHorizontal: Spacing.three,
    minHeight: 56,
  },
  menuLabel: {
    flex: 1,
  },
});
