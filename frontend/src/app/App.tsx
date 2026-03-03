import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireAuth } from './auth/RequireAuth'
import LoginPage from '../features/login/LoginPage'
import UserShell from './layout/UserShell'
import SupportShell from './layout/SupportShell'
import TransactionsPage from '../features/transactions/TransactionsPage'
import TransactionDetailsPage from '../features/transactions/TransactionDetailsPage'
import DisputesPage from '../features/disputes/DisputesPage'
import DisputeDetailsPage from '../features/disputes/DisputeDetailsPage'
import SupportDisputesPage from '../features/support/SupportDisputesPage'
import SupportDisputeDetailsPage from '../features/support/SupportDisputeDetailsPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<RequireAuth />}>
        <Route path="/" element={<Navigate to="/transactions" replace />} />

        <Route element={<UserShell />}>
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route path="/transactions/:transactionId" element={<TransactionDetailsPage />} />
          <Route path="/disputes" element={<DisputesPage />} />
          <Route path="/disputes/:disputeId" element={<DisputeDetailsPage />} />
        </Route>

        <Route element={<SupportShell />}>
          <Route path="/support/disputes" element={<SupportDisputesPage />} />
          <Route path="/support/disputes/:disputeId" element={<SupportDisputeDetailsPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
