import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../app/auth/useAuth'
import { apiFetch, ApiError } from '../../app/api/http'
import Banner from '../../shared/components/Banner'
import Spinner from '../../shared/components/Spinner'
import { DisputeStatus, DisputeViewRow, PageResponse, TransactionStatus } from '../../shared/types/domain'

function startOfDayInstant(dateYYYYMMDD: string): string {
  return `${dateYYYYMMDD}T00:00:00.000Z`
}
function endOfDayInstant(dateYYYYMMDD: string): string {
  return `${dateYYYYMMDD}T23:59:59.999Z`
}

const DISPUTE_STATUS_OPTIONS: Array<{ label: string; value: DisputeStatus }> = [
  { label: 'OPEN', value: 'OPEN' },
  { label: 'IN_PROGRESS', value: 'IN_PROGRESS' },
  { label: 'RESOLVED', value: 'RESOLVED' },
  { label: 'REJECTED', value: 'REJECTED' },
]

const TRANSACTION_STATUS_OPTIONS: Array<{ label: string; value: TransactionStatus }> = [
  { label: 'POSTED', value: 'POSTED' },
  { label: 'PENDING', value: 'PENDING' },
  { label: 'REVERSED', value: 'REVERSED' },
  { label: 'SETTLED', value: 'SETTLED' },
]

export default function DisputesPage() {
  const { auth } = useAuth()

  const [pageData, setPageData] = useState<PageResponse<DisputeViewRow> | null>(null)
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)

  const [merchant, setMerchant] = useState('')
  const [disputeStatus, setDisputeStatus] = useState<DisputeStatus | ''>('')
  const [transactionStatus, setTransactionStatus] = useState<TransactionStatus | ''>('')
  const [rrn, setRrn] = useState('')
  const [minAmount, setMinAmount] = useState('')
  const [maxAmount, setMaxAmount] = useState('')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')

  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)

  const query = useMemo(() => {
    const q: Record<string, string | number> = { page, size }
    if (merchant) q.merchant = merchant
    if (disputeStatus) q.disputeStatus = disputeStatus
    if (transactionStatus) q.transactionStatus = transactionStatus
    if (rrn) q.rrn = rrn
    if (minAmount) q.minAmount = minAmount
    if (maxAmount) q.maxAmount = maxAmount
    if (fromDate) q.fromDate = startOfDayInstant(fromDate)
    if (toDate) q.toDate = endOfDayInstant(toDate)
    return q
  }, [merchant, disputeStatus, transactionStatus, rrn, minAmount, maxAmount, fromDate, toDate, page, size])

  async function load() {
    setLoading(true)
    setErr(null)
    try {
      const data = await apiFetch<PageResponse<DisputeViewRow>>('/v1/disputes/view', { auth, query })
      setPageData(data)
    } catch (e: any) {
      const ae = e as ApiError
      setErr(ae.detail ?? ae.title ?? 'Failed to load disputes')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query])

  const rows = pageData?.items ?? []

  return (
    <div>
      <h2>My Disputes</h2>
      {err && <Banner kind="error" message={err} />}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8, marginBottom: 12 }}>
        <input placeholder="Merchant" value={merchant} onChange={e => { setPage(0); setMerchant(e.target.value) }} />

        <select value={disputeStatus} onChange={e => { setPage(0); setDisputeStatus(e.target.value as any) }}>
          <option value="">Any Dispute Status</option>
          {DISPUTE_STATUS_OPTIONS.map(o => (<option key={o.value} value={o.value}>{o.label}</option>))}
        </select>

        <select value={transactionStatus} onChange={e => { setPage(0); setTransactionStatus(e.target.value as any) }}>
          <option value="">Any Transaction Status</option>
          {TRANSACTION_STATUS_OPTIONS.map(o => (<option key={o.value} value={o.value}>{o.label}</option>))}
        </select>

        <input placeholder="RRN" value={rrn} onChange={e => { setPage(0); setRrn(e.target.value) }} />

        <div className="inline">
          <input placeholder="Min amount" value={minAmount} onChange={e => { setPage(0); setMinAmount(e.target.value) }} />
          <input placeholder="Max amount" value={maxAmount} onChange={e => { setPage(0); setMaxAmount(e.target.value) }} />
        </div>

        <input placeholder="From date (YYYY-MM-DD)" value={fromDate} onChange={e => { setPage(0); setFromDate(e.target.value) }} />
        <input placeholder="To date (YYYY-MM-DD)" value={toDate} onChange={e => { setPage(0); setToDate(e.target.value) }} />
        <div style={{ gridColumn: '1 / -1', display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 6 }}>
          <button className="btn btn-primary" onClick={() => { setPage(0); void load() }}>
            Search
          </button>
          <button className="btn" onClick={() => {
          setMerchant('')
          setDisputeStatus('')
          setTransactionStatus('')
          setRrn('')
          setMinAmount('')
          setMaxAmount('')
          setFromDate('')
          setToDate('')
          setPage(0)
          void load()
          }}>
            Clear
          </button>
        </div>
      </div>

      {pageData && (
        <div className="toolbar">
          <div>
            {pageData.totalElements === 0
              ? 'No results'
              : <>Showing {(pageData.page * pageData.size) + 1}–{Math.min((pageData.page + 1) * pageData.size, pageData.totalElements)} of {pageData.totalElements}</>
            }
          </div>
          <div className="toolbar-right">
            <span className="label-inline">
              Page size
              <select value={size} onChange={e => { setPage(0); setSize(Number(e.target.value)) }}>
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
              </select>
            </span>
            <button className="btn" disabled={!pageData.hasPrevious} onClick={() => setPage(p => Math.max(0, p - 1))}>Prev</button>
            <span className="label-inline">Page {pageData.page + 1} / {Math.max(pageData.totalPages, 1)}</span>
            <button className="btn" disabled={!pageData.hasNext} onClick={() => setPage(p => p + 1)}>Next</button>
          </div>
        </div>
      )}

      {loading ? (
        <Spinner />
      ) : (
        <table width="100%" cellPadding={8} style={{ borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              <th align="left">Dispute</th>
              <th align="left">Transaction</th>
              <th align="left">Merchant</th>
              <th align="left">RRN</th>
              <th align="right">Amount</th>
              <th align="left">Dispute Status</th>
              <th align="left">Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map(r => (
              <tr key={r.disputeId} >
                <td>{r.disputeId}</td>
                <td>{r.transactionId}</td>
                <td>{r.merchant}</td>
                <td>{r.rrn ?? ''}</td>
                <td align="right">{r.amount} {r.currency}</td>
                <td>{r.disputeStatus}</td>
                <td><Link to={`/disputes/${r.disputeId}`}>View</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
