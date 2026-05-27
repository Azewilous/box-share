import client from './client'

export interface RegisterPayload {
  firstName: string
  lastName: string
  email: string
  username: string
  password: string
}

export interface LoginPayload {
  email: string
  password: string
}

export interface MeResponse {
  email: string
  role: string
  emailVerified: boolean
}

export async function register(payload: RegisterPayload): Promise<void> {
  await client.post('/api/auth/register', payload)
}

export async function login(payload: LoginPayload): Promise<void> {
  await client.post('/api/auth/login', payload)
}

export async function logout(): Promise<void> {
  await client.post('/api/auth/logout')
}

export async function me(): Promise<MeResponse> {
  const { data } = await client.get<MeResponse>('/api/auth/me')
  return data
}

export async function verifyEmail(token: string): Promise<void> {
  await client.post(`/api/user/verify/${token}`)
}

export async function resendVerification(): Promise<void> {
  await client.post('/api/user/verify/resend')
}
