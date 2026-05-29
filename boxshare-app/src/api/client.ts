import axios from 'axios'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
})

let _onAuthFailure: (() => void) | null = null

export function setOnAuthFailure(cb: () => void) {
  _onAuthFailure = cb
}

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (import.meta.env.DEV) {
      const { config, response } = error
      console.group(`[API ERROR] ${config?.method?.toUpperCase()} ${config?.url}`)
      console.log('Status :', response?.status ?? 'no response')
      console.log('Body   :', response?.data ?? error.message)
      console.groupEnd()
    }
    if (error.response?.status === 401) {
      _onAuthFailure?.()
    }
    return Promise.reject(error)
  },
)

export default client
