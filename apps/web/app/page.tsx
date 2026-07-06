import { createFikaliakoClient, type PingResponse } from '@fikaliako/api-client';

const api = createFikaliakoClient(process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080');

async function fetchPing(): Promise<PingResponse | null> {
  try {
    const { data } = await api.GET('/v1/ping', { cache: 'no-store' });
    return data ?? null;
  } catch {
    return null;
  }
}

export default async function Home() {
  const ping = await fetchPing();

  return (
    <main>
      <h1>This is the home screen</h1>
      {ping ? (
        <p>
          {ping.service} says: {ping.message}
        </p>
      ) : (
        <p>API is unreachable — start it with `pnpm turbo dev --filter=api`.</p>
      )}
    </main>
  );
}
