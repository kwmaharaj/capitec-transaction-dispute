// React escapes strings by default, but keep this helper to avoid future "render HTML" mistakes.
export function asPlainText(v: unknown): string {
  if (v === null || v === undefined) return ''
  return String(v)
}
