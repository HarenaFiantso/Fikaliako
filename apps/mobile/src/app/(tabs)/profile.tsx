import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { ActivityIndicator, Alert, Platform, Pressable, StyleSheet, View } from 'react-native';
import Animated, { FadeInDown } from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/button';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';

import { useTheme } from '@/hooks/use-theme';

import { formatPhone } from '@/lib/auth/phone';
import { useSession } from '@/lib/auth/session-store';

import { BottomTabInset, MaxContentWidth, Spacing } from '@/constants/theme';

export default function ProfileScreen() {
  const status = useSession((state) => state.status);

  return (
    <ThemedView style={styles.root}>
      <SafeAreaView style={styles.safeArea}>
        {status === 'restoring' && <ActivityIndicator style={styles.flex} />}
        {status === 'signedOut' && <SignedOutView />}
        {status === 'signedIn' && <SignedInView />}
      </SafeAreaView>
    </ThemedView>
  );
}

function SignedOutView() {
  const router = useRouter();
  const theme = useTheme();

  return (
    <View style={styles.signedOut}>
      <Animated.View entering={FadeInDown.duration(300)} style={styles.hero}>
        <View style={[styles.heroIcon, { backgroundColor: theme.backgroundElement }]}>
          <Ionicons name="person-outline" size={40} color={theme.textSecondary} />
        </View>
        <ThemedText type="subtitle" style={styles.centered}>
          Your account
        </ThemedText>
        <ThemedText type="default" themeColor="textSecondary" style={styles.centered}>
          Sign in to save favorites, rate the places you eat at and put new gargottes on the map.
        </ThemedText>
      </Animated.View>
      <Animated.View entering={FadeInDown.duration(300).delay(80)} style={styles.actions}>
        <Button title="Sign in" onPress={() => router.push('/sign-in')} />
        <Button
          title="Create an account"
          variant="secondary"
          onPress={() => router.push('/sign-up')}
        />
      </Animated.View>
    </View>
  );
}

function SignedInView() {
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
    <View style={styles.signedIn}>
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
    </View>
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
  },
  flex: {
    flex: 1,
  },
  centered: {
    textAlign: 'center',
  },
  signedOut: {
    flex: 1,
    justifyContent: 'center',
    gap: Spacing.five,
  },
  hero: {
    alignItems: 'center',
    gap: Spacing.three,
  },
  heroIcon: {
    width: 80,
    height: 80,
    borderRadius: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  actions: {
    gap: Spacing.three,
  },
  signedIn: {
    flex: 1,
    gap: Spacing.five,
    paddingTop: Spacing.six,
  },
  identity: {
    alignItems: 'center',
    gap: Spacing.two,
  },
  avatar: {
    width: 72,
    height: 72,
    borderRadius: 36,
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
    gap: Spacing.two,
  },
  menuRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    borderRadius: Spacing.three,
    paddingHorizontal: Spacing.three,
    minHeight: 56,
  },
  menuLabel: {
    flex: 1,
  },
});
