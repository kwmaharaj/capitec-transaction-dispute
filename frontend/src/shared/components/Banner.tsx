export default function Banner({ kind, message }: { kind: 'error' | 'info', message: string }) {
  const bg = kind === 'error' ? '#ffe5e5' : '#e7f2ff'
  const border = kind === 'error' ? '#ff9b9b' : '#8fc0ff'
  return (
    <div style={{ padding: 10, background: bg, border: `1px solid ${border}`, marginBottom: 12 }}>
      {message}
    </div>
  )
}
