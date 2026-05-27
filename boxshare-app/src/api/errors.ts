import axios from 'axios'

export function resolveApiError(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const status = err.response?.status
    if (status === 400) return 'Please check your details and try again.'
    if (status === 401) return 'Invalid email or password.'
    if (status === 403) return 'Access denied.'
    if (status === 409) return 'An account with this email already exists.'
    if (status != null && status >= 500) return 'Server error — please try again later.'
    if (!err.response) return 'Could not reach the server. Check your connection.'
  }
  return 'Something went wrong. Please try again.'
}
