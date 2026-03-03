import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useAuth } from '../../app/auth/useAuth'
import { apiFetch, ApiError } from '../../app/api/http'
import Banner from '../../shared/components/Banner'
import Spinner from '../../shared/components/Spinner'
import { DisputeHistoryRow, DisputeViewRow, PageResponse } from '../../shared/types/domain'

type DecisionRequest = {
  disputeStatus: string
  supportNote: string
}

export default function SupportDisputeDetailsPage() {
  const { disputeId } = useParams()
  const { auth } = useAuth()

  const [row, setRow] = useState<DisputeViewRow | null>(null)
  const [history, setHistory] = useState<DisputeHistoryRow[]>([])
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)

  const [decisionStatus, setDecisionStatus] = useState('RESOLVED')
  const [supportNote, setSupportNote] = useState('')
  const [saving, setSaving] = useState(false)
  const [saveErr, setSaveErr] = useState<string | null>(null)

  async function load() {
    if (!disputeId) return
    setLoading(true)
    setErr(null)
    try {
      // IMPORTANT: view endpoints are now paged; filter by disputeId
      const view = await apiFetch<PageResponse<DisputeViewRow>>('/v1/support/disputes/view', {
        auth,
        query: { disputeId, page: 0, size: 1 },
      })
      setRow(view.items[0] ?? null)

      const h = await apiFetch<DisputeHistoryRow[]>(`/v1/support/disputes/${disputeId}/history`, { auth })
      setHistory(h)
    } catch (e: any) {
      const ae = e as ApiError
      setErr(ae.detail ?? ae.title ?? 'Failed to load dispute')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [disputeId])

  async function submitDecision() {
    if (!disputeId) return
    setSaving(true)
    setSaveErr(null)
    try {
      const body: DecisionRequest = { disputeStatus: decisionStatus, supportNote }
      await apiFetch(`/v1/support/disputes/${disputeId}/decision`, {
        auth,
        method: 'POST',
        body,
      })
      setSupportNote('')
      await load()
    } catch (e: any) {
      const ae = e as ApiError
      setSaveErr(ae.detail ?? ae.title ?? 'Failed to submit decision')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div>
      <h2>Support Dispute Details</h2>
      {err && <Banner kind="error" message={err} />}

      {loading ? (
        <Spinner />
      ) : !row ? (
        <Banner kind="warning" message="Dispute not found" />
      ) : (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12, marginBottom: 16 }}>
            <div>
              <div><b>Dispute ID:</b> {row.disputeId}</div>
              <div><b>User ID:</b> {row.userId}</div>
              <div><b>Dispute Status:</b> {row.disputeStatus}</div>
              <div><b>Note:</b> {row.disputeNote ?? ""}</div>
              <div><b>Created:</b> {row.createdAt}</div>
            </div>
            <div>
              <div><b>Transaction ID:</b> {row.transactionId}</div>
              <div><b>Merchant:</b> {row.merchant}</div>
              <div><b>RRN:</b> {row.rrn ?? ""}</div>
              <div><b>Amount:</b> {row.amount} {row.currency}</div>
            </div>
          </div>

          <h3>Decision</h3>
          {saveErr && <Banner kind="error" message={saveErr} />}
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 8 }}>
            <label>
              Status{' '}
              <select value={decisionStatus} onChange={e => setDecisionStatus(e.target.value)}>
                <option value="IN_PROGRESS">IN_PROGRESS</option>
                <option value="RESOLVED">RESOLVED</option>
                <option value="REJECTED">REJECTED</option>
              </select>
            </label>
            <button className="btn" disabled={saving} onClick={submitDecision}>
              {saving ? 'Saving…' : 'Submit'}
            </button>
          </div>
          <textarea
            placeholder="Support note"
            value={supportNote}
            onChange={e => setSupportNote(e.target.value)}
            rows={3}
            style={{ width: '100%', marginBottom: 16 }}
          />

          <h3>History</h3>
          {history.length === 0 ? (
            <div>No history</div>
          ) : (
            <table width="100%" cellPadding={8} style={{ borderCollapse: 'collapse' }}>
              <thead>
              <tr>
                <th align="left">When</th>
                <th align="left">From → To</th>
                <th align="left">Note</th>
                <th align="left">Actor</th>
              </tr>
              </thead>
              <tbody>
              {history.map((h, idx) => (
                <tr key={idx} style={{ borderTop: '1px solid #eee' }}>
                  <td>{h.changedAt}</td>
                  <td>{`${h.fromDisputeStatus ?? ""} → ${h.toDisputeStatus}`}</td>
                  <td>{h.note ?? ""}</td>
                  <td>{h.actorUserId}</td>
                </tr>
              ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </div>
  )
}