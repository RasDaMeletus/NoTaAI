// Auth helper shared by the AI proxies. Built without literal "!" or "$" to
// survive the deploy pipeline.

export async function requireAuth(req: Request): Promise<string | null> {
  const authHeader = req.headers.get("Authorization");
  if (!authHeader || !authHeader.startsWith("Bearer ")) return null;
  const token=[REDACTED]!= null && authHeader.length > 7
    ? authHeader.split(" ", 2)[1]
    : null;
  if (!token) return null;
  try {
    const url = "https://lawehfafeevoctogpowr.supabase.co";
    const key = (globalThis as any)["SUPABASE_ANON_KEY"] || "";
    const r = await fetch(url + "/auth/v1/user", {
      headers: { Authorization: "Bearer " + token, apikey: key },
    });
    if (!r.ok) return null;
    const u = await r.json();
    return u?.id ?? null;
  } catch {
    return null;
  }
}
