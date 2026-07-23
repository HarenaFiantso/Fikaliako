import { StyleSheet, View } from 'react-native';

import type { OpeningInterval } from '@fikaliako/api-client';

import { ThemedText } from '@/components/themed-text';

import { Spacing } from '@/constants/theme';

const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

export function OpeningHours({ intervals }: { intervals: OpeningInterval[] }) {
  // Book ch. 6.1: day 0 = Monday, while JS getDay() has 0 = Sunday.
  const today = (new Date().getDay() + 6) % 7;

  return (
    <View style={styles.list}>
      {DAYS.map((day, index) => {
        const slots = intervals
          .filter((interval) => interval.day_of_week === index)
          .sort((a, b) => a.opens_at.localeCompare(b.opens_at))
          .map((interval) => `${interval.opens_at} – ${interval.closes_at}`)
          .join(', ');
        const isToday = index === today;
        const type = isToday ? 'smallBold' : 'small';

        return (
          <View key={day} style={styles.row}>
            <ThemedText type={type} themeColor={isToday ? 'text' : 'textSecondary'}>
              {day}
            </ThemedText>
            <ThemedText type={type} themeColor={isToday && slots ? 'text' : 'textSecondary'}>
              {slots || 'Closed'}
            </ThemedText>
          </View>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  list: {
    gap: Spacing.two,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: Spacing.three,
  },
});
