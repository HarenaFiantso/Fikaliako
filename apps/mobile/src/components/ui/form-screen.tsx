import { type ReactNode } from 'react';

import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet } from 'react-native';
import Animated, { FadeInDown } from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';

import { useTheme } from '@/hooks/use-theme';

import { MaxContentWidth, Spacing } from '@/constants/theme';

export type FormScreenProps = {
  title: string;
  subtitle?: string;
  children: ReactNode;
};

export function FormScreen({ title, subtitle, children }: FormScreenProps) {
  const theme = useTheme();
  const router = useRouter();

  return (
    <ThemedView style={styles.root}>
      <SafeAreaView style={styles.safeArea} edges={['top', 'bottom', 'left', 'right']}>
        <KeyboardAvoidingView
          style={styles.flex}
          behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        >
          <ScrollView
            contentContainerStyle={styles.content}
            keyboardShouldPersistTaps="handled"
            showsVerticalScrollIndicator={false}
          >
            {router.canGoBack() && (
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Go back"
                onPress={() => router.back()}
                style={[styles.backButton, { backgroundColor: theme.backgroundElement }]}
              >
                <Ionicons name="chevron-back" size={22} color={theme.text} />
              </Pressable>
            )}
            <Animated.View entering={FadeInDown.duration(300)} style={styles.header}>
              <ThemedText type="subtitle">{title}</ThemedText>
              {subtitle ? (
                <ThemedText type="default" themeColor="textSecondary">
                  {subtitle}
                </ThemedText>
              ) : null}
            </Animated.View>
            <Animated.View entering={FadeInDown.duration(300).delay(80)} style={styles.body}>
              {children}
            </Animated.View>
          </ScrollView>
        </KeyboardAvoidingView>
      </SafeAreaView>
    </ThemedView>
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
  },
  flex: {
    flex: 1,
  },
  content: {
    flexGrow: 1,
    padding: Spacing.four,
    gap: Spacing.four,
  },
  backButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  header: {
    gap: Spacing.two,
  },
  body: {
    gap: Spacing.three,
  },
});
