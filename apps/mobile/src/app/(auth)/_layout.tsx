import { Stack } from 'expo-router';

export const unstable_settings = {
  initialRouteName: 'sign-in',
};

export default function AuthLayout() {
  return (
    <Stack
      screenOptions={{
        headerShown: false,
        animation: 'slide_from_right',
      }}
    />
  );
}
