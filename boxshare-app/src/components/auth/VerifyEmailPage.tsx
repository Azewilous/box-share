import { useEffect, useState } from 'react'
import GcButton from '../button/GcButton'
import s from './VerifyEmailPage.module.css'
import { verifyEmail } from '../../api/auth'

interface Props {
  token: string
}

type Status = 'loading' | 'success' | 'error'

export default function VerifyEmailPage({ token }: Props) {
  const [status, setStatus] = useState<Status>('loading')

  useEffect(() => {
    verifyEmail(token)
      .then(() => setStatus('success'))
      .catch(() => setStatus('error'))
  }, [token])

  function goHome() {
    window.location.href = '/'
  }

  return (
    <div className={s.page}>
      <div className={s.card}>
        {status === 'loading' && (
          <>
            <div className={s.spinner} />
            <p className={`${s.title} ${s.loading}`}>Verifying…</p>
            <p className={s.text}>Confirming your email address.</p>
          </>
        )}

        {status === 'success' && (
          <>
            <div className={s.icon}>✅</div>
            <p className={`${s.title} ${s.success}`}>Email Verified!</p>
            <p className={s.text}>Your account is now active. You can log in and start using BoxShare.</p>
            <div className={s.actions}>
              <GcButton variant="a" label="Go to App" onClick={goHome} />
            </div>
          </>
        )}

        {status === 'error' && (
          <>
            <div className={s.icon}>❌</div>
            <p className={`${s.title} ${s.error}`}>Link Invalid</p>
            <p className={s.text}>This verification link is invalid or has expired. Please register again or contact support.</p>
            <div className={s.actions}>
              <GcButton variant="b" label="Go to App" onClick={goHome} />
            </div>
          </>
        )}
      </div>
    </div>
  )
}
