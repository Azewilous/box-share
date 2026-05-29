import { describe, it, expect } from 'vitest'
import axios from 'axios'
import { resolveApiError } from '../api/errors'

function makeAxiosError(status: number) {
  const err = new axios.AxiosError('Request failed')
  err.response = { status, data: {}, headers: {}, config: err.config!, statusText: '' }
  return err
}

describe('resolveApiError', () => {
  it('returns bad-input message for 400', () => {
    expect(resolveApiError(makeAxiosError(400))).toBe('Please check your details and try again.')
  })

  it('returns credentials message for 401', () => {
    expect(resolveApiError(makeAxiosError(401))).toBe('Invalid email or password.')
  })

  it('returns access denied for 403', () => {
    expect(resolveApiError(makeAxiosError(403))).toBe('Access denied.')
  })

  it('returns conflict message for 409', () => {
    expect(resolveApiError(makeAxiosError(409))).toBe('An account with this email already exists.')
  })

  it('returns server error for 500', () => {
    expect(resolveApiError(makeAxiosError(500))).toBe('Server error — please try again later.')
  })

  it('returns server error for 503', () => {
    expect(resolveApiError(makeAxiosError(503))).toBe('Server error — please try again later.')
  })

  it('returns network error when no response', () => {
    const err = new axios.AxiosError('Network Error')
    // no response set — simulates connection refused
    expect(resolveApiError(err)).toBe('Could not reach the server. Check your connection.')
  })

  it('returns generic message for unknown errors', () => {
    expect(resolveApiError(new Error('Unexpected'))).toBe('Something went wrong. Please try again.')
    expect(resolveApiError('string error')).toBe('Something went wrong. Please try again.')
    expect(resolveApiError(null)).toBe('Something went wrong. Please try again.')
  })
})
