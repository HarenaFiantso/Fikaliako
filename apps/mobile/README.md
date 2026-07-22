# Fikaliako mobile

React Native / Expo app (Expo SDK 57, expo-router) — the mobile client planned in project book ch. 7, sharing `@fikaliako/api-client` with the web app and rendering maps with MapLibre Native.

## Run

```sh
pnpm turbo dev --filter=mobile   # or: pnpm --filter mobile dev  (expo start)
pnpm --filter mobile ios         # iOS simulator
pnpm --filter mobile android     # Android emulator
```

MapLibre is a native module, so the app needs a development build rather than Expo Go:

```sh
pnpm --filter mobile prebuild    # generate android/ + ios/ (gitignored)
pnpm --filter mobile ios         # then build & run
```

## API

The app talks to `apps/api` through `src/lib/api.ts`. Configure the base URL with `EXPO_PUBLIC_API_URL` (see `.env.example`); it defaults to `http://localhost:8080`, which only works on simulators — use your LAN IP for physical devices, `http://10.0.2.2:8080` for Android emulators.

## Checks

```sh
pnpm --filter mobile lint          # ESLint (repo base config + eslint-config-expo)
pnpm --filter mobile check-types   # tsc --noEmit
```
