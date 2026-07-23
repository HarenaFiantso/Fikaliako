# Fikaliako mobile

React Native / Expo app (Expo SDK 57, expo-router) — the mobile client planned in project book ch. 7, sharing `@fikaliako/api-client` with the web app and rendering maps with MapLibre Native.

## Run

MapLibre is a native module, so the app runs in a development build, never Expo Go. Compile and install the build once per native change, then iterate against the dev server:

```sh
pnpm --filter mobile ios         # expo run:ios — native build + install + start (simulator)
pnpm --filter mobile android     # expo run:android — same for the Android emulator
pnpm turbo dev --filter=mobile   # expo start --dev-client — JS-only iteration afterwards
```

`expo run:*` regenerates the native projects (`ios/`, `android/`, both gitignored) as needed; `pnpm --filter mobile prebuild` does that step alone.

## API

The app talks to `apps/api` through `src/lib/api.ts`. Configure the base URL with `EXPO_PUBLIC_API_URL` (see `.env.example`); it defaults to `http://localhost:8080`, which only works on simulators — use your LAN IP for physical devices, `http://10.0.2.2:8080` for Android emulators.

## Checks

```sh
pnpm --filter mobile lint          # ESLint (repo base config + eslint-config-expo)
pnpm --filter mobile check-types   # tsc --noEmit
```
