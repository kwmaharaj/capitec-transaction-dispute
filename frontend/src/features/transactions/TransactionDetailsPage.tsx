import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../../app/auth/useAuth'
import { apiFetch, ApiError } from '../../app/api/http'
import Banner from '../../shared/components/Banner'
import Spinner from '../../shared/components/Spinner'
import { TransactionView } from '../../shared/types/domain'

export default function TransactionDetailsPage() {
  const { auth } = useAuth()
  const { transactionId } = useParams()
  const nav = useNavigate()

  const [tx, setTx] = useState<TransactionView | null>(null)
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)

  const [show, setShow] = useState(false)
  const [note, setNote] = useState('')
  const [reasonCode, setReasonCode] = useState('FRAUD_SUSPECTED')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    async function load() {
      setLoading(true)
      setErr(null)
      try {
        const data = await apiFetch<TransactionView>(`/v1/transactions/${transactionId}`, { auth })
        setTx(data)
      } catch (e: any) {
        const ae = e as ApiError
        setErr(ae.detail ?? ae.title ?? 'Failed to load')
      } finally {
        setLoading(false)
      }
    }
    void load()
  }, [auth, transactionId])

  async function createDispute() {
    if (!transactionId) return
    setSaving(true)
    setErr(null)
    try {
      await apiFetch('/v1/disputes', {
        method: 'POST',
        auth,
        body: { transactionId, reasonCode, note },
      })
      nav('/disputes', { replace: true })
    } catch (e: any) {
      const ae = e as ApiError
      setErr(ae.detail ?? ae.title ?? 'Failed to create dispute')
    } finally {
      setSaving(false)
      setShow(false)
    }
  }

  return (
    <div>
      <h2>Transaction Details</h2>
      {err && <Banner kind="error" message={err} />}
      {loading ? <Spinner /> : tx && (
        <div className="card">
          <div><strong>transactionId:</strong> {tx.transactionId}</div>
          <div><strong>postedAt:</strong> {tx.postedAt ?? ''}</div>
          <div><strong>merchant:</strong> {tx.merchant}</div>
          <div><strong>amount:</strong> {tx.amount} {tx.currency}</div>
          <div><strong>transactionStatus:</strong> {tx.transactionStatus}</div>
          <div><strong>rrn:</strong> {tx.rrn ?? ''}</div>

          <div style={{ marginTop: 12 }}>
            <button className="btn btn-primary" onClick={() => setShow(true)}>Dispute</button>
          </div>
        </div>
      )}

      {show && (
        <div style={{ marginTop: 12, border: '1px solid #ddd', padding: 12 }}>
          <h3>Create Dispute</h3>
          <label>
            Reason
            <select value={reasonCode} onChange={e => setReasonCode(e.target.value)} style={{ marginLeft: 8 }}>
              <option value="DUPLICATE">DUPLICATE</option>
              <option value="FRAUD_SUSPECTED">FRAUD_SUSPECTED</option>
              <option value="GOODS_NOT_RECEIVED">GOODS_NOT_RECEIVED</option>
              <option value="INCORRECT_AMOUNT">INCORRECT_AMOUNT</option>
              <option value="OTHER">OTHER</option>
            </select>
          </label>
          <div style={{ marginTop: 8 }}>
            <label>
              Note
              <textarea value={note} onChange={e => setNote(e.target.value)} rows={4} style={{ width: '100%' }} maxLength={500} />
            </label>
          </div>
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            <button className="btn" onClick={() => setShow(false)} disabled={saving}>Cancel</button>
            <button onClick={createDispute} disabled={saving}>Create Dispute</button>
          </div>
        </div>
      )}
    </div>
  )
}
