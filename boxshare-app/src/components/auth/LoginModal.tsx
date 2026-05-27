import { useState } from 'react'
import Modal from '../modal/Modal'
import GcButton from '../button/GcButton'
import s from './Auth.module.css'

interface Props {
  onClose: () => void
  onSwitchToRegister: () => void
}

interface Fields { email: string; password: string }
interface Errors { email?: string; password?: string }

function validate(fields: Fields): Errors {
  const errors: Errors = {}
  if (!fields.email)                        errors.email    = 'Email is required'
  else if (!/\S+@\S+\.\S+/.test(fields.email)) errors.email = 'Enter a valid email'
  if (!fields.password)                     errors.password = 'Password is required'
  return errors
}

export default function LoginModal({ onClose, onSwitchToRegister }: Props) {
  const [fields, setFields] = useState<Fields>({ email: '', password: '' })
  const [errors, setErrors] = useState<Errors>({})

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setFields(prev => ({ ...prev, [e.target.name]: e.target.value }))
    setErrors(prev => ({ ...prev, [e.target.name]: undefined }))
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const errs = validate(fields)
    if (Object.keys(errs).length) { setErrors(errs); return }
    // TODO: call POST /api/auth/login
    console.log('login', fields)
  }

  return (
    <Modal title="— LOG IN —" onClose={onClose}>
      <form className={s.form} onSubmit={handleSubmit} noValidate>
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
            autoComplete="current-password"
          />
          {errors.password && <span className={s.errorMsg}>{errors.password}</span>}
        </div>

        <div className={s.actions}>
          <GcButton variant="b" label="Cancel" onClick={onClose} />
          <GcButton variant="a" label="Log In" type="submit" />
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
