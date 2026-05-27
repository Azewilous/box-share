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
    if (error.response?.status === 401) {
      _onAuthFailure?.()
    }
    return Promise.reject(error)
  },
)

export default client
