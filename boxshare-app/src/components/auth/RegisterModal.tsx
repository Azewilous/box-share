import { useState } from 'react'
import Modal from '../modal/Modal'
import GcButton from '../button/GcButton'
import s from './Auth.module.css'

interface Props {
  onClose: () => void
  onSwitchToLogin: () => void
}

interface Fields {
  username: string
  email: string
  password: string
  confirmPassword: string
}

interface Errors {
  username?: string
  email?: string
  password?: string
  confirmPassword?: string
}

function validate(fields: Fields): Errors {
  const errors: Errors = {}
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

export default function RegisterModal({ onClose, onSwitchToLogin }: Props) {
  const [fields, setFields] = useState<Fields>({
    username: '', email: '', password: '', confirmPassword: '',
  })
  const [errors, setErrors] = useState<Errors>({})

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setFields(prev => ({ ...prev, [e.target.name]: e.target.value }))
    setErrors(prev => ({ ...prev, [e.target.name]: undefined }))
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const errs = validate(fields)
    if (Object.keys(errs).length) { setErrors(errs); return }
    // TODO: call POST /api/auth/register
    console.log('register', fields)
  }

  return (
    <Modal title="— NEW TRAINER —" onClose={onClose} maxWidth={420}>
      <form className={s.form} onSubmit={handleSubmit} noValidate>
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

        <div className={s.actions}>
          <GcButton variant="b" label="Cancel"   onClick={onClose} />
          <GcButton variant="a" label="Register" type="submit" />
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
