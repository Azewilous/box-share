import { useState } from 'react'
import Modal from '../modal/Modal'
import GcButton from '../button/GcButton'
import s from './Auth.module.css'
import { useAuth } from '../../context/useAuth'
import { resolveApiError } from '../../api/errors'

interface Props {
  onClose: () => void
  onSwitchToRegister: () => void
  onSuccess: () => void
}

interface Fields { email: string; password: string }
interface Errors { email?: string; password?: string }

function validate(fields: Fields): Errors {
  const errors: Errors = {}
  if (!fields.email)                            errors.email    = 'Email is required'
  else if (!/\S+@\S+\.\S+/.test(fields.email)) errors.email    = 'Enter a valid email'
  if (!fields.password)                         errors.password = 'Password is required'
  return errors
}

export default function LoginModal({ onClose, onSwitchToRegister, onSuccess }: Props) {
  const { login } = useAuth()
  const [fields, setFields] = useState<Fields>({ email: '', password: '' })
  const [errors, setErrors] = useState<Errors>({})
  const [apiError, setApiError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setFields(prev => ({ ...prev, [e.target.name]: e.target.value }))
    setErrors(prev => ({ ...prev, [e.target.name]: undefined }))
    setApiError(null)
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const errs = validate(fields)
    if (Object.keys(errs).length) { setErrors(errs); return }

    setLoading(true)
    setApiError(null)
    try {
      await login(fields)
      onSuccess()
    } catch (err) {
      setApiError(resolveApiError(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal title="— LOG IN —" onClose={onClose}>
      <form className={s.form} onSubmit={handleSubmit} noValidate>
        <div className={s.field}>
          <label className={s.label}>Email</label>
          <input
            className={`${s.input} ${errors.email ? s.error : ''}`}
            type="text"
            inputMode="email"
            name="email"
            placeholder="trainer@example.com"
            value={fields.email}
            onChange={handleChange}
            autoComplete="email"
            autoCorrect="off"
            autoCapitalize="none"
            spellCheck={false}
          />
          {errors.email && <span className={s.errorMsg}>{errors.email}</span>}
        </div>

        <div className={s.field}>
          <label className={s.label}>Password</label>
          <input
            className={`${s.input} ${errors.password ? s.error : ''}`}
            type="password"
            name="password"
            placeholder="••••••••"
            value={fields.password}
            onChange={handleChange}
            autoComplete="current-password"
          />
          {errors.password && <span className={s.errorMsg}>{errors.password}</span>}
        </div>

        {apiError && <p className={s.apiError}>{apiError}</p>}

        <div className={s.actions}>
          <GcButton variant="b" label="Cancel" onClick={onClose} disabled={loading} />
          <GcButton variant="a" label={loading ? 'Logging in…' : 'Log In'} type="submit" disabled={loading} />
        </div>

        <p className={s.footer}>
          No account?{' '}
          <button type="button" className={s.footerLink} onClick={onSwitchToRegister}>
            Register here
          </button>
        </p>
      </form>
    </Modal>
  )
}
