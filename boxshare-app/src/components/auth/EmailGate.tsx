import { useState, useEffect } from 'react'
import GcButton from '../button/GcButton'
import s from './EmailGate.module.css'
import { useAuth } from '../../context/useAuth'
import { resendVerification } from '../../api/auth'
import axios from 'axios'

const COOLDOWN_SECONDS = 60

export default function EmailGate() {
  const { user, logout } = useAuth()
  const [cooldown, setCooldown]   = useState(0)
  const [statusMsg, setStatusMsg] = useState<{ text: string; ok: boolean } | null>(null)
  const [sending, setSending]     = useState(false)

  useEffect(() => {
    if (cooldown <= 0) return
    const id = setTimeout(() => setCooldown(c => c - 1), 1000)
    return () => clearTimeout(id)
  }, [cooldown])

  async function handleResend() {
    setSending(true)
    setStatusMsg(null)
    try {
      await resendVerification()
      setCooldown(COOLDOWN_SECONDS)
      setStatusMsg({ text: 'Verification email sent! Check your inbox.', ok: true })
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 429) {
        setCooldown(COOLDOWN_SECONDS)
        setStatusMsg({ text: 'Please wait before requesting another email.', ok: false })
      } else {
        setStatusMsg({ text: 'Something went wrong. Please try again.', ok: false })
      }
    } finally {
      setSending(false)
    }
  }

  const resendLabel = sending
    ? 'Sending…'
    : cooldown > 0
      ? `Resend in ${cooldown}s`
      : 'Resend Email'

  return (
    <div className={s.gate}>
      <div className={s.card}>
        <div className={s.icon}>📧</div>

        <p className={s.title}>Verify Your Email</p>

        <p className={s.text}>
          We sent a verification link to{' '}
          <span className={s.email}>{user?.email}</span>.
          <br />
          Click the link in your inbox to access BoxShare.
        </p>

        <p className={s.note}>The link expires in 24 hours.</p>

        {statusMsg && (
          <p className={statusMsg.ok ? s.msgOk : s.msgErr}>{statusMsg.text}</p>
        )}

        <div className={s.actions}>
          <GcButton variant="x" label={resendLabel} onClick={handleResend} disabled={cooldown > 0 || sending} />
          <GcButton variant="b" label="Log Out"     onClick={logout} />
        </div>
      </div>
    </div>
  )
}
