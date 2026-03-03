export type TransactionStatus = 'POSTED' | 'PENDING' | 'REVERSED' | 'SETTLED'
export type DisputeStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED'

export type TransactionView = {
  transactionId: string
  userId: string
  postedAt?: string | null
  amount: number
  currency: string
  merchant: string
  transactionStatus: TransactionStatus
  rrn?: string | null
  bin?: string | null
  last4digits?: string | null
  panHash?: string | null
}

export type DisputeViewRow = {
  disputeId: string
  transactionId: string
  userId: string
  disputeStatus: DisputeStatus
  reasonCode: string
  disputeNote?: string | null
  createdAt: string
  postedAt?: string | null
  amount: number
  currency: string
  merchant: string
  transactionStatus: TransactionStatus
  rrn?: string | null
}

export type DisputeHistoryRow = {
  historyId: string
  disputeId: string
  fromDisputeStatus?: DisputeStatus | null
  toDisputeStatus: DisputeStatus
  note?: string | null
  actorRole: string
  actorUserId: string
  changedAt: string
}

export type PageResponse<T> = {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}
