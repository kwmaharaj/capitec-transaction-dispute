type IdemState = 'in_progress' | 'completed'

type StoredIdem = {
  key: string
  state: IdemState
  ts: number // epoch millis (last state change)
}

// How long we keep keys around.
// - in_progress: long enough to survive retries / reloads while the user is still trying.
// - completed: short grace window to protect against accidental double-submit right after success.
const IN_PROGRESS_TTL_MS = 10 * 60 * 1000
const COMPLETED_TTL_MS = 2 * 60 * 1000

function now(): number {
  return Date.now()
}

function getTtlMs(state: IdemState): number {
  return state === 'completed' ? COMPLETED_TTL_MS : IN_PROGRESS_TTL_MS
}

function isExpired(entry: StoredIdem): boolean {
  return (now() - entry.ts) > getTtlMs(entry.state)
}

function safeParse(jsonStr: string | null): StoredIdem | null {
  if (!jsonStr) return null
  try {
    const x = JSON.parse(jsonStr)
    if (!x || typeof x !== 'object') return null
    if (typeof x.key !== 'string') return null
    if (x.state !== 'in_progress' && x.state !== 'completed') return null
    if (typeof x.ts !== 'number') return null
    return x as StoredIdem
  } catch {
    return null
  }
}

function randomKey(): string {
  // Most modern browsers
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }

  // Fallback (RFC4122-ish) if randomUUID isn't available
  const bytes = new Uint8Array(16)
  crypto.getRandomValues(bytes)
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = Array.from(bytes).map(b => b.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

function encodeUtf8(s: string): Uint8Array {
  return new TextEncoder().encode(s)
}

async function sha256Hex(input: string): Promise<string> {
  const data = encodeUtf8(input)
  const digest = await crypto.subtle.digest('SHA-256', data)
  const bytes = new Uint8Array(digest)
  return Array.from(bytes).map(b => b.toString(16).padStart(2, '0')).join('')
}

// Deterministic JSON string for hashing.
// - sort object keys
// - preserve array order
// - drop undefined values (JSON.stringify does as well)
function stableStringify(value: unknown): string {
  if (value === null) return 'null'
  const t = typeof value
  if (t === 'number' || t === 'boolean') return JSON.stringify(value)
  if (t === 'string') return JSON.stringify(value)
  if (t !== 'object') return 'null'

  if (Array.isArray(value)) {
    return '[' + value.map(v => stableStringify(v)).join(',') + ']'
  }

  const obj = value as Record<string, unknown>
  const keys = Object.keys(obj).sort()
  const parts: string[] = []
  for (const k of keys) {
    const v = obj[k]
    if (v === undefined) continue
    parts.push(JSON.stringify(k) + ':' + stableStringify(v))
  }
  return '{' + parts.join(',') + '}'
}

// Builds a storage key that is stable for "the same user action" across reloads.
// We don't want the raw signature as the key (can be long); we hash it.
async function storageKey(signature: string): Promise<string> {
  const sigHash = await sha256Hex(signature)
  return `idem:${sigHash}`
}

export function isMutatingMethod(method: string): boolean {
  switch (method.toUpperCase()) {
    case 'POST':
    case 'PUT':
    case 'PATCH':
    case 'DELETE':
      return true
    default:
      return false
  }
}

/**
 * Get (or create) an idempotency key for a mutating request.
 *
 * Strategy:
 * - Create a deterministic signature from (method, URL, body).
 * - Store a generated key in sessionStorage so reloads / retries reuse it.
 * - Keep it "in_progress" until a successful response, then mark "completed" briefly.
 */
export async function getOrCreateIdempotencyKey(params: {
  method: string
  url: string
  body?: unknown
}): Promise<{ key: string; storageKey: string }> {
  const method = params.method.toUpperCase()

  // Signature needs to be stable, but not overly sensitive to irrelevant differences.
  const bodySig = params.body === undefined ? '' : stableStringify(params.body)
  const signature = `v1|${method}|${params.url}|${bodySig}`

  const sk = await storageKey(signature)
  const existing = safeParse(sessionStorage.getItem(sk))
  if (existing && !isExpired(existing)) {
    return { key: existing.key, storageKey: sk }
  }

  const created: StoredIdem = { key: randomKey(), state: 'in_progress', ts: now() }
  sessionStorage.setItem(sk, JSON.stringify(created))
  return { key: created.key, storageKey: sk }
}

/**
 * Mark the idempotency key as completed (successful response) so a quick reload won't double-submit,
 * but a later intentional repeat action can still generate a new key.
 */
export function markIdempotencyCompleted(storageKey: string): void {
  const existing = safeParse(sessionStorage.getItem(storageKey))
  if (!existing) return
  const updated: StoredIdem = { ...existing, state: 'completed', ts: now() }
  sessionStorage.setItem(storageKey, JSON.stringify(updated))
}

/**
 * Optional cleanup helper if you ever want to purge expired entries.
 * Not used by default to avoid extra overhead on each request.
 */
export function purgeExpiredIdempotencyKeys(): void {
  try {
    const keys: string[] = []
    for (let i = 0; i < sessionStorage.length; i++) {
      const k = sessionStorage.key(i)
      if (k && k.startsWith('idem:')) keys.push(k)
    }
    for (const k of keys) {
      const v = safeParse(sessionStorage.getItem(k))
      if (v && isExpired(v)) sessionStorage.removeItem(k)
    }
  } catch {
    // ignore
  }
}