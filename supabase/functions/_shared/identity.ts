// Shared auth for the edge functions.
//
// The Android client passes its user access token in the `apikey` header. The
// platform runtime cannot resolve the project anon key for `Authorization: ***
// (it throws `ReferenceError: SUPABASE_ANON_KEY is not defined`), so Bearer is
// avoided on the client side.
//
// A publishable key alone never authenticates: it ships inside the APK, so it
// is public by definition. Only a real user access token is accepted, verified
// against the project ES256 signing key via the JWKS endpoint.

const PROJECT_URL = "https://lawehfafeevoctogpowr.supabase.co";
const JWKS_URL = `${PROJECT_URL}/auth/v1/.well-known/jwks.json`;

// The publishable key is compiled into the APK. It identifies the project but
// authenticates nobody, so it is rejected outright as an identity token.
const PUBLISHABLE_KEYS = ["sb_publishable_jnUEZPBGCSFfOCqUtySjHA_Jf9wsxt2"];

// The JWT issuer is the project URL.
// Supabase uses the GoTrue base path as the issuer claim, e.g.
// https://<project>.supabase.co/auth/v1
const ISSUER = `${PROJECT_URL}/auth/v1`;

type Jwk = { kid?: string; [k: string]: unknown };
type JwksCache = { keys: Jwk[]; fetchedAt: number };

let jwksCache: JwksCache | null = null;
const JWKS_TTL_MS = 5 * 60_000;

async function loadJwks(): Promise<Jwk[]> {
  if (jwksCache && Date.now() - jwksCache.fetchedAt < JWKS_TTL_MS) {
    return jwksCache.keys;
  }
  const r = await fetch(JWKS_URL);
  if (!r.ok) throw new Error(`JWKS fetch failed: ${r.status}`);
  const jwks = (await r.json()) as { keys?: Jwk[] };
  if (!Array.isArray(jwks.keys) || jwks.keys.length === 0) {
    throw new Error("JWKS has no keys");
  }
  jwksCache = { keys: jwks.keys, fetchedAt: Date.now() };
  return jwksCache.keys;
}

function b64urlDecode(input: string): Uint8Array {
  const pad = "=".repeat((4 - (input.length % 4)) % 4);
  const b64 = (input + pad).replace(/-/g, "+").replace(/_/g, "/");
  // atob is available in the Deno/edge runtime.
  const raw = atob(b64);
  const out = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i++) out[i] = raw.charCodeAt(i);
  return out;
}

async function importKey(jwk: Jwk): Promise<CryptoKey> {
  // Supabase ES256 keys are JWK; subtleCrypto accepts them directly.
  return await crypto.subtle.importKey(
    "jwk",
    jwk as JsonWebKey,
    { name: "ECDSA", namedCurve: "P-256" },
    false,
    ["verify"],
  );
}

// Segment decoder: returns null on malformed input rather than throwing, so a
// hostile token cannot crash the caller.
function decodeSegment(seg: string): Record<string, unknown> | null {
  try {
    const bytes = b64urlDecode(seg);
    return JSON.parse(new TextDecoder().decode(bytes)) as Record<string, unknown>;
  } catch {
    return null;
  }
}

// Verify an ES256 JWT against the project published signing key.
// Returns the subject (user id) when signature, issuer, and expiry check out;
// null otherwise.
async function verifyJwt(token: string): Promise<string | null> {
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  const [headerB64, payloadB64, sigB64] = parts;

  const header = decodeSegment(headerB64);
  if (header == null) return null;
  if (header.alg !== "ES256" || header.typ !== "JWT") return null;

  const payload = decodeSegment(payloadB64);
  if (payload == null) return null;

  const now = Math.floor(Date.now() / 1000);
  if (typeof payload.exp === "number" && now >= payload.exp) return null;
  if (typeof payload.iat === "number" && payload.iat > now + 60) return null;
  if (payload.iss !== ISSUER) return null;

  const kid = typeof header.kid === "string" ? header.kid : undefined;
  let keys: Jwk[];
  try {
    keys = await loadJwks();
  } catch {
    return null;
  }
  const jwk = kid ? keys.find((k) => k.kid === kid) : keys[0];
  if (jwk == null) return null;

  const key = await importKey(jwk);

  const sig = b64urlDecode(sigB64);
  const data = new TextEncoder().encode(`${headerB64}.${payloadB64}`);
  // ECDSA P-256 over SHA-256. The JWS signature is raw r||s, which is what
  // WebCrypto expects for this curve.
  const ok = await crypto.subtle.verify(
    { name: "ECDSA", hash: "SHA-256" },
    key,
    sig,
    data,
  );
  if (!ok) return null;

  return typeof payload.sub === "string" ? payload.sub : null;
}

// Public entry point. Resolves to the authenticated user id, or null when the
// token is absent, expired, malformed, or signed by a different key.
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

  // A publishable key identifies the project but authenticates nobody.
  if (PUBLISHABLE_KEYS.includes(token)) return null;

  return await verifyJwt(token);
}
