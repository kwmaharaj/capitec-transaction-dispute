import { AuthState } from '../auth/AuthProvider'
import { getOrCreateIdempotencyKey, isMutatingMethod, markIdempotencyCompleted } from './idempotency'

export type ApiError = {
  status: number
  title?: string
  detail?: string
  instance?: string
  [k: string]: unknown
}

type ApiEnvelope<T> = {
  success: boolean
  data: T
}

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'


function isEnvelope(x: any): x is ApiEnvelope<any> {
  return x && typeof x === 'object' && typeof x.success === 'boolean' && 'data' in x
}

export async function apiFetch<T>(path: string, opts: {
  method?: string
  auth?: AuthState | null
  body?: unknown
  query?: Record<string, string | number | boolean | undefined | null>
} = {}): Promise<T> {

  const url = new URL(path, API_BASE)
  if (opts.query) {
    for (const [k, v] of Object.entries(opts.query)) {
      if (v === undefined || v === null || v === '') continue
      url.searchParams.set(k, String(v))
    }
  }

  const headers: Record<string, string> = {
    'Accept': 'application/json',
  }

  if (opts.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  if (opts.auth?.accessToken) {
    headers['Authorization'] = `Bearer ${opts.auth.accessToken}`
  }

  const method = (opts.method ?? 'GET').toUpperCase()

  // Add idempotency key for all mutating requests.
  // - key is stable across reloads/retries for the same action signature (method+url+body)
  // - on success we mark it as completed for a short grace period to prevent accidental double-submits
  let idempotencyStorageKey: string | null = null
  if (isMutatingMethod(method)) {
    const idem = await getOrCreateIdempotencyKey({
      method,
      url: url.toString(),
      body: opts.body,
    })
    idempotencyStorageKey = idem.storageKey
    headers['X-Idempotency-Key'] = idem.key
  }

  const res = await fetch(url.toString(), {
    method,
    headers,
    body: opts.body === undefined ? undefined : JSON.stringify(opts.body),
  })

  if (!res.ok) {
    let errBody: any = null
    try { errBody = await res.json() } catch { /* ignore */ }
    const apiErr: ApiError = {
      status: res.status,
      ...(errBody ?? {}),
    }
    throw apiErr
  }

  if (idempotencyStorageKey) {
    markIdempotencyCompleted(idempotencyStorageKey)
  }

  if (res.status === 204) return undefined as T
  const body = await res.json()
    //handle standard envelope: { success: true, data: ... }
    if (isEnvelope(body)) {
      if (!body.success) {
        const apiErr: ApiError = { status: res.status, title: 'Request failed', detail: 'Request failed' }
        throw apiErr
      }
     return body.data as T
    }

  // Fallback: allow non-enveloped endpoints (e.g., dev tools) without breaking the app
  return body as T
}