import { useState } from 'react'
import Modal from '../modal/Modal'
import GcButton from '../button/GcButton'
import type { FileRecord } from '../../api/files'
import s from './ShareModal.module.css'

type ShareTab = 'link' | 'email'

interface Props {
  file: FileRecord
  onClose: () => void
  onGenerateLink: () => Promise<FileRecord>
  onRevokeLink: () => Promise<FileRecord>
  onEmailShare: (email: string) => Promise<void>
  onFileUpdate: (updated: FileRecord) => void
}

export default function ShareModal({ file, onClose, onGenerateLink, onRevokeLink, onEmailShare, onFileUpdate }: Props) {
  const [tab,       setTab]       = useState<ShareTab>('link')
  const [email,     setEmail]     = useState('')
  const [busy,      setBusy]      = useState(false)
  const [copied,    setCopied]    = useState(false)
  const [emailSent, setEmailSent] = useState(false)
  const [error,     setError]     = useState<string | null>(null)

  const isPublic = file.visibility === 'PUBLIC'
  const siteLink = file.shareToken
    ? `${window.location.origin}/view/${file.shareToken}`
    : null

  async function run(fn: () => Promise<void>) {
    setError(null)
    setBusy(true)
    try { await fn() } catch { setError('Something went wrong. Try again.') }
    finally { setBusy(false) }
  }

  async function handleGenerateLink() {
    await run(async () => {
      const updated = await onGenerateLink()
      onFileUpdate(updated)
    })
  }

  async function handleRevoke() {
    await run(async () => {
      const updated = await onRevokeLink()
      onFileUpdate(updated)
    })
  }

  async function copyLink() {
    if (!siteLink) return
    await navigator.clipboard.writeText(siteLink)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  async function handleEmailShare() {
    if (!email.trim()) return
    await run(async () => {
      await onEmailShare(email.trim())
      setEmail('')
      setEmailSent(true)
      setTimeout(() => setEmailSent(false), 2500)
    })
  }

  return (
    <Modal title={`Share — ${file.name}`} onClose={onClose} maxWidth={380}>
      <div className={s.wrap}>
        <div className={s.tabs}>
          <button className={[s.tab, tab === 'link'  ? s.active : ''].join(' ')} onClick={() => setTab('link')}>
            Link Share
          </button>
          <button className={[s.tab, tab === 'email' ? s.active : ''].join(' ')} onClick={() => setTab('email')}>
            Email Share
          </button>
        </div>

        {tab === 'link' && (
          <div className={s.section}>
            {!isPublic ? (
              <>
                <p className={s.hint}>Generate a public link anyone can use to view this file.</p>
                <div className={s.actions}>
                  <GcButton variant="a" label="Generate Link" onClick={handleGenerateLink} disabled={busy} />
                </div>
              </>
            ) : (
              <>
                <p className={s.hint}>Anyone with this link can view the file.</p>
                <div className={s.linkRow}>
                  <span className={s.linkText}>{siteLink}</span>
                  <button className={s.copyBtn} onClick={copyLink}>
                    {copied ? '✓ Copied' : 'Copy'}
                  </button>
                </div>
                <div className={s.actions}>
                  <GcButton variant="b" label="Revoke Link" onClick={handleRevoke} disabled={busy} />
                </div>
              </>
            )}
          </div>
        )}

        {tab === 'email' && (
          <div className={s.section}>
            <p className={s.hint}>Share directly with a BoxShare user by their email address.</p>
            <input
              className={s.input}
              type="email"
              placeholder="user@example.com"
              value={email}
              onChange={e => setEmail(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && !busy && handleEmailShare()}
              disabled={busy}
            />
            <div className={s.actions}>
              <GcButton
                variant="a"
                label={emailSent ? '✓ Shared!' : 'Share'}
                onClick={handleEmailShare}
                disabled={busy || !email.trim()}
              />
            </div>
          </div>
        )}

        {error && <p className={s.error}>{error}</p>}
      </div>
    </Modal>
  )
}
