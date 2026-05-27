import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'
import {
  login as apiLogin,
  register as apiRegister,
  logout as apiLogout,
  me as apiMe,
  type LoginPayload,
  type RegisterPayload,
} from '../api/auth'
import { setOnAuthFailure } from '../api/client'

const SESSION_KEY = 'bs_session'

export interface AuthUser {
  email: string
  role: string
  isVerified: boolean
}

interface AuthContextValue {
  user: AuthUser | null
  isLoggedIn: boolean
  isVerified: boolean
  isLoading: boolean
  login: (payload: LoginPayload) => Promise<void>
  register: (payload: RegisterPayload) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [isLoading, setIsLoading] = useState(() => localStorage.getItem(SESSION_KEY) === 'true')

  const clearSession = useCallback(() => {
    localStorage.removeItem(SESSION_KEY)
    setUser(null)
  }, [])

  const applyMe = useCallback((data: { email: string; role: string; emailVerified: boolean }) => {
    setUser({ email: data.email, role: data.role, isVerified: data.emailVerified })
  }, [])

  // validate cookie and restore user info on mount
  useEffect(() => {
    if (localStorage.getItem(SESSION_KEY) !== 'true') return

    apiMe()
      .then(applyMe)
      .catch(clearSession)
      .finally(() => setIsLoading(false))
  }, [applyMe, clearSession])

  useEffect(() => {
    setOnAuthFailure(clearSession)
  }, [clearSession])

  const login = useCallback(async (payload: LoginPayload) => {
    await apiLogin(payload)
    const data = await apiMe()
    localStorage.setItem(SESSION_KEY, 'true')
    applyMe(data)
  }, [applyMe])

  const register = useCallback(async (payload: RegisterPayload) => {
    await apiRegister(payload)
    const data = await apiMe()
    localStorage.setItem(SESSION_KEY, 'true')
    applyMe(data)
  }, [applyMe])

  const logout = useCallback(async () => {
    try { await apiLogout() } catch { /* cookie cleared server-side regardless */ }
    clearSession()
  }, [clearSession])

  return (
    <AuthContext.Provider value={{
      user,
      isLoggedIn: !!user,
      isVerified: user?.isVerified ?? false,
      isLoading,
      login,
      register,
      logout,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
