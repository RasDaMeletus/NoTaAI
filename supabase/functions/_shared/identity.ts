// Shared auth for the edge functions. The Android client passes its token in the apikey header; the
// platform runtime cannot resolve the project anon key for Authorization: Bearer requests,
// so Bearer is avoided on the client side. A publishable key alone never authenticates:
// it ships inside the APK. Only a real user access token is accepted,
// verified against the project auth service.

export async function requireAuth(req: Request): Promise<string | null> {
  const authHeader = req.headers.get("Authorization");
  const apiHeader = req.headers.get("apikey");
  let token: string | null = null;
  if (authHeader != null && authHeader.startsWith("Bearer ")) {
    token = authHeader.split(" ", 2)[1];
  } else if (apiHeader != null && apiHeader.length > 40) {
    token = apiHeader;
  }
  if (token == null) return null;
  try {
    const API_KEY = "sb_publishable_jnUEZPBGCSFfOCqUtySjHA_Jf9wsxt2";
    const r = await fetch("https://lawehfafeevoctogpowr.supabase.co/auth/v1/user", {
      headers: { apikey: API_KEY, Authorization: "Bearer " + token },
    });
    if (!r.ok) return null;
    const u = await r.json();
    return u?.id ?? null;
  } catch {
    return null;
  }
}

