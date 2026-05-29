import { useState, useEffect } from 'react'
import Modal from '../modal/Modal'
import GcButton from '../button/GcButton'
import s from './Auth.module.css'
import { fetchProfile, updateProfile, type UserProfile } from '../../api/users'
import { resolveApiError } from '../../api/errors'
import { useAuth } from '../../context/useAuth'

interface Props {
  onClose: () => void
}

export default function ProfileModal({ onClose }: Props) {
  const { user } = useAuth()
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [fields, setFields] = useState({ firstName: '', lastName: '', email: '' })
  const [apiError, setApiError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    if (!user) return
    fetchProfile(user.email)
      .then(p => {
        setProfile(p)
        setFields({ firstName: p.firstName ?? '', lastName: p.lastName ?? '', email: p.email })
      })
      .catch(() => setApiError('Failed to load profile.'))
      .finally(() => setLoading(false))
  }, [user])

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setFields(prev => ({ ...prev, [e.target.name]: e.target.value }))
    setApiError(null)
    setSaved(false)
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!profile) return
    setSaving(true)
    setApiError(null)
    try {
      const updated = await updateProfile({ ...profile, ...fields })
      setProfile(updated)
      setSaved(true)
    } catch (err) {
      setApiError(resolveApiError(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title="— TRAINER PROFILE —" onClose={onClose} maxWidth={420}>
      {loading ? (
        <p style={{ textAlign: 'center', color: 'var(--text-dim)', padding: '24px 0' }}>Loading…</p>
      ) : (
        <form className={s.form} onSubmit={handleSubmit} noValidate>
          <div className={s.row}>
            <div className={s.field}>
              <label className={s.label}>First Name</label>
              <input
                className={s.input}
                type="text"
                name="firstName"
                value={fields.firstName}
                onChange={handleChange}
                autoComplete="given-name"
              />
            </div>
            <div className={s.field}>
              <label className={s.label}>Last Name</label>
              <input
                className={s.input}
                type="text"
                name="lastName"
                value={fields.lastName}
                onChange={handleChange}
                autoComplete="family-name"
              />
            </div>
          </div>

          <div className={s.field}>
            <label className={s.label}>Email</label>
            <input
              className={s.input}
              type="email"
              name="email"
              value={fields.email}
              onChange={handleChange}
              autoComplete="email"
            />
          </div>

          <div className={s.field}>
            <label className={s.label}>Role</label>
            <input
              className={s.input}
              type="text"
              value={user?.role ?? '—'}
              readOnly
              style={{ opacity: 0.6, cursor: 'default' }}
            />
          </div>

          {apiError && <p className={s.apiError}>{apiError}</p>}
          {saved    && <p className={s.savedMsg}>Profile updated!</p>}

          <div className={s.actions}>
            <GcButton variant="b" label="Close"  onClick={onClose}  disabled={saving} />
            <GcButton variant="a" label={saving ? 'Saving…' : 'Save'} type="submit" disabled={saving} />
          </div>
        </form>
      )}
    </Modal>
  )
}
