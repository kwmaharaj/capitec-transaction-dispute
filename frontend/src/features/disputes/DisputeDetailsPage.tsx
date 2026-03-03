import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useAuth } from '../../app/auth/useAuth'
import { apiFetch, ApiError } from '../../app/api/http'
import Banner from '../../shared/components/Banner'
import Spinner from '../../shared/components/Spinner'
import { DisputeHistoryRow, DisputeViewRow, PageResponse } from '../../shared/types/domain'

export default function DisputeDetailsPage() {
  const { auth } = useAuth()
  const { disputeId } = useParams()

  const [row, setRow] = useState<DisputeViewRow | null>(null)
  const [hist, setHist] = useState<DisputeHistoryRow[]>([])
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)

  useEffect(() => {
    async function load() {
      if (!disputeId) return
      setLoading(true)
      setErr(null)
      try {
        const view = await apiFetch<PageResponse<DisputeViewRow>>('/v1/disputes/view', {
          auth,
          query: { disputeId, page: 0, size: 1 },
        })
        setRow(view.items[0] ?? null)

        const history = await apiFetch<DisputeHistoryRow[]>(`/v1/disputes/${disputeId}/history`, { auth })
        setHist(history)
      } catch (e: any) {
        const ae = e as ApiError
        setErr(ae.detail ?? ae.title ?? 'Failed to load')
      } finally {
        setLoading(false)
      }
    }
    void load()
  }, [auth, disputeId])

  return (
    <div>
      <h2>Dispute Details</h2>
      {err && <Banner kind="error" message={err} />}
      {loading ? <Spinner /> : (
        <>
          {row && (
            <div style={{ border: '1px solid #eee', padding: 12, marginBottom: 12 }}>
              <div><strong>disputeId:</strong> {row.disputeId}</div>
              <div><strong>transactionId:</strong> {row.transactionId}</div>
              <div><strong>merchant:</strong> {row.merchant}</div>
              <div><strong>amount:</strong> {row.amount} {row.currency}</div>
              <div><strong>txStatus:</strong> {row.transactionStatus}</div>
              <div><strong>disputeStatus:</strong> {row.disputeStatus}</div>
              <div><strong>reasonCode:</strong> {row.reasonCode}</div>
              <div><strong>note:</strong> {row.disputeNote ?? ""}</div>
            </div>
          )}

          <h3>History</h3>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
            <tr>
              <th align="left">ChangedAt</th>
              <th align="left">Actor</th>
              <th align="left">From</th>
              <th align="left">To</th>
              <th align="left">Note</th>
            </tr>
            </thead>
            <tbody>
            {hist.map(h => (
              <tr key={h.historyId} style={{ borderTop: '1px solid #eee' }}>
                <td>{h.changedAt}</td>
                <td>{h.actorRole}</td>
                <td>{h.fromDisputeStatus ?? ''}</td>
                <td>{h.toDisputeStatus}</td>
                <td>{h.note ?? ''}</td>
              </tr>
            ))}
            </tbody>
          </table>
        </>
      )}
    </div>
  )
}
