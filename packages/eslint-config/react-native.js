import expoConfig from 'eslint-config-expo/flat.js';
import eslintConfigPrettier from 'eslint-config-prettier';
import onlyWarn from 'eslint-plugin-only-warn';
import turboPlugin from 'eslint-plugin-turbo';

/**
 * A custom ESLint configuration for Expo / React Native apps.
 *
 * Builds on eslint-config-expo — which already wires typescript-eslint and
 * the React plugins — rather than ./base.js: layering both would register
 * the @typescript-eslint plugin twice, which flat config rejects.
 *
 * @type {import("eslint").Linter.Config[]}
 * */
export const reactNativeConfig = [
  ...expoConfig,
  eslintConfigPrettier,
  {
    // Reanimated shared values are mutated through `.value` inside event
    // handlers and effects — a pattern this rule cannot model.
    rules: {
      'react-hooks/immutability': 'off',
    },
  },
  {
    plugins: {
      turbo: turboPlugin,
    },
    rules: {
      'turbo/no-undeclared-env-vars': 'warn',
    },
  },
  {
    plugins: {
      onlyWarn,
    },
  },
  {
    ignores: ['dist/**', '.expo/**', 'android/**', 'ios/**', 'expo-env.d.ts'],
  },
];
