const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

type PingResponse = {
  service: string;
  message: string;
};

async function fetchPing(): Promise<PingResponse | null> {
  try {
    const res = await fetch(`${API_URL}/v1/ping`, { cache: "no-store" });
    if (!res.ok) return null;
    return (await res.json()) as PingResponse;
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
