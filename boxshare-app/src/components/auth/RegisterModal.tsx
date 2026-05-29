import { useState } from 'react'
import Modal from '../modal/Modal'
import GcButton from '../button/GcButton'
import s from './Auth.module.css'
import { useAuth } from '../../context/useAuth'
import { resolveApiError } from '../../api/errors'
import PasswordStrength from './PasswordStrength'

interface Props {
  onClose: () => void
  onSwitchToLogin: () => void
  onSuccess: () => void
}

interface Fields {
  firstName: string
  lastName: string
  username: string
  email: string
  password: string
  confirmPassword: string
}

interface Errors {
  firstName?: string
  lastName?: string
  username?: string
  email?: string
  password?: string
  confirmPassword?: string
}

function validate(fields: Fields): Errors {
  const errors: Errors = {}
  if (!fields.firstName || fields.firstName.length < 1)
    errors.firstName = 'First name is required'
  if (!fields.lastName || fields.lastName.length < 1)
    errors.lastName = 'Last name is required'
  if (!fields.username || fields.username.length < 3)
    errors.username = 'Username must be at least 3 characters'
  if (!fields.email || !/\S+@\S+\.\S+/.test(fields.email))
    errors.email = 'Enter a valid email'
  if (!fields.password || fields.password.length < 6)
    errors.password = 'Password must be at least 6 characters'
  if (fields.confirmPassword !== fields.password)
    errors.confirmPassword = 'Passwords do not match'
  return errors
}

export default function RegisterModal({ onClose, onSwitchToLogin, onSuccess }: Props) {
  const { register } = useAuth()
  const [fields, setFields] = useState<Fields>({
    firstName: '', lastName: '', username: '', email: '', password: '', confirmPassword: '',
  })
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
      await register({
        firstName: fields.firstName,
        lastName: fields.lastName,
        username: fields.username,
        email: fields.email,
        password: fields.password,
      })
      onSuccess()
    } catch (err) {
      setApiError(resolveApiError(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal title="— NEW TRAINER —" onClose={onClose} maxWidth={420}>
      <form className={s.form} onSubmit={handleSubmit} noValidate>
        <div className={s.row}>
          <div className={s.field}>
            <label className={s.label}>First Name</label>
            <input
              className={`${s.input} ${errors.firstName ? s.error : ''}`}
              type="text"
              name="firstName"
              placeholder="Ash"
              value={fields.firstName}
              onChange={handleChange}
              autoComplete="given-name"
            />
            {errors.firstName && <span className={s.errorMsg}>{errors.firstName}</span>}
          </div>

          <div className={s.field}>
            <label className={s.label}>Last Name</label>
            <input
              className={`${s.input} ${errors.lastName ? s.error : ''}`}
              type="text"
              name="lastName"
              placeholder="Ketchum"
              value={fields.lastName}
              onChange={handleChange}
              autoComplete="family-name"
            />
            {errors.lastName && <span className={s.errorMsg}>{errors.lastName}</span>}
          </div>
        </div>

        <div className={s.field}>
          <label className={s.label}>Username</label>
          <input
            className={`${s.input} ${errors.username ? s.error : ''}`}
            type="text"
            name="username"
            placeholder="AshKetchum"
            value={fields.username}
            onChange={handleChange}
            autoComplete="username"
          />
          {errors.username && <span className={s.errorMsg}>{errors.username}</span>}
        </div>

        <div className={s.field}>
          <label className={s.label}>Email</label>
          <input
            className={`${s.input} ${errors.email ? s.error : ''}`}
            type="email"
            name="email"
            placeholder="trainer@example.com"
            value={fields.email}
            onChange={handleChange}
            autoComplete="email"
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
            autoComplete="new-password"
          />
          {errors.password && <span className={s.errorMsg}>{errors.password}</span>}
        </div>

        <PasswordStrength password={fields.password} />

        <div className={s.field}>
          <label className={s.label}>Confirm Password</label>
          <input
            className={`${s.input} ${errors.confirmPassword ? s.error : ''}`}
            type="password"
            name="confirmPassword"
            placeholder="••••••••"
            value={fields.confirmPassword}
            onChange={handleChange}
            autoComplete="new-password"
          />
          {errors.confirmPassword && <span className={s.errorMsg}>{errors.confirmPassword}</span>}
        </div>

        {apiError && <p className={s.apiError}>{apiError}</p>}

        <div className={s.actions}>
          <GcButton variant="b" label="Cancel"   onClick={onClose} disabled={loading} />
          <GcButton variant="a" label={loading ? 'Registering…' : 'Register'} type="submit" disabled={loading} />
        </div>

        <p className={s.footer}>
          Already have an account?{' '}
          <button type="button" className={s.footerLink} onClick={onSwitchToLogin}>
            Log in here
          </button>
        </p>
      </form>
    </Modal>
  )
}
