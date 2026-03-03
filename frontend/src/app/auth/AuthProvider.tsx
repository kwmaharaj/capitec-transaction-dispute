import React, { createContext, useEffect, useMemo, useState } from 'react'

export type Role = 'USER' | 'SUPPORT'

export type AuthState = {
  accessToken: string
  userId: string
  roles: Role[]
}

type AuthContextValue = {
  auth: AuthState | null
  login: (next: AuthState) => void
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

const STORAGE_KEY = 'txdispute.auth'

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [auth, setAuth] = useState<AuthState | null>(null)

  useEffect(() => {
    // sessionStorage only (avoid localStorage)
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return
    try {
      const parsed = JSON.parse(raw) as AuthState
      if (parsed?.accessToken && parsed?.userId) setAuth(parsed)
    } catch {
      // ignore
    }
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    auth,
    login: (next) => {
      setAuth(next)
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    },
    logout: () => {
      setAuth(null)
      sessionStorage.removeItem(STORAGE_KEY)
    },
  }), [auth])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
