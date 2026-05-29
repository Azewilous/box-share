import { useState, useRef, useCallback, useEffect } from 'react'
import mockSlots from './data/mockSlots'
import TopBar from './components/layout/TopBar'
import ControlsBar from './components/layout/ControlsBar'
import FileDataPanel from './components/pc/FileDataPanel'
import BoxArea from './components/pc/BoxArea'
import LoginModal from './components/auth/LoginModal'
import RegisterModal from './components/auth/RegisterModal'
import ProfileModal from './components/auth/ProfileModal'
import EmailGate from './components/auth/EmailGate'
import VerifyEmailPage from './components/auth/VerifyEmailPage'
import FileViewerModal from './components/pc/FileViewerModal'
import ShareModal from './components/pc/ShareModal'
import PublicViewer from './components/pc/PublicViewer'
import Modal from './components/modal/Modal'
import GcButton from './components/button/GcButton'
import { useAuth } from './context/useAuth'
import { listMyFiles, initiateUpload, updateVisibility, deleteFile, shareFileWithUser, type FileRecord } from './api/files'
import axios from 'axios'
import { fileRecordToSlot } from './utils/fileUtils'
import type { FileSlot } from './types/file'
import s from './App.module.css'

type Modal = 'login' | 'register' | 'profile' | 'viewer' | 'confirm-delete' | 'share' | null
type Tab   = 'party' | 'box'

const BOX_SIZE   = 30
const PARTY_SIZE = 6
const COLS       = 6

function padSlots(files: (FileSlot | null)[], size: number): (FileSlot | null)[] {
  return Array.from({ length: size }, (_, i) => files[i] ?? null)
}

const VERIFY_TOKEN = window.location.pathname === '/verify-email'
  ? new URLSearchParams(window.location.search).get('token')
  : null

const PUBLIC_VIEW_TOKEN = window.location.pathname.startsWith('/view/')
  ? window.location.pathname.slice('/view/'.length)
  : null

export default function App() {
  const { user, isLoggedIn, isVerified, isLoading, logout } = useAuth()

  const [files,         setFiles]         = useState<FileRecord[]>([])
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null)
  const [activeTab,     setActiveTab]     = useState<Tab>('box')
  const [modal,         setModal]         = useState<Modal>(null)
  const [uploading,     setUploading]     = useState(false)
  const [activeKey,     setActiveKey]     = useState<string | null>(null)

  const uploadRef   = useRef<HTMLInputElement>(null)

  const gridSize     = activeTab === 'party' ? PARTY_SIZE : BOX_SIZE
  const gridSizeRef = useRef(gridSize)
  gridSizeRef.current = gridSize
  const displaySlots: (FileSlot | null)[] = isLoggedIn
    ? padSlots(files.map(fileRecordToSlot), gridSize)
    : padSlots(mockSlots, gridSize)

  const selectedFile = selectedIndex !== null ? files[selectedIndex] ?? null : null
  const selectedSlot = selectedIndex !== null ? displaySlots[selectedIndex] ?? null : null
const hasSelection = selectedIndex !== null && displaySlots[selectedIndex] !== null

  function closeModal() { setModal(null) }

  function handleTabChange(tab: Tab) {
    setActiveTab(tab)
    setSelectedIndex(null)
  }

  function handleSelect(index: number) {
    setSelectedIndex(prev => prev === index ? null : index)
  }

  // ── Load files on login ───────────────────────────────────────────
  useEffect(() => {
    if (!isLoggedIn || !isVerified) { setFiles([]); return }
    listMyFiles().then(setFiles).catch(console.error)
  }, [isLoggedIn, isVerified])

  // ── Upload ────────────────────────────────────────────────────────
  const triggerUpload = useCallback(() => {
    uploadRef.current?.click()
  }, [])

  const handleFileChange = useCallback(async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file || !user) return
    e.target.value = ''

    setUploading(true)
    try {
      const record = await initiateUpload({
        name:       file.name,
        size:       file.size,
        mimeType:   file.type || 'application/octet-stream',
        uploadedBy: user.email,
        status:     'NOT_STARTED',
        visibility: 'PRIVATE',
      })
      if (record.presignedUrl) {
        await fetch(record.presignedUrl, { method: 'PUT', body: file })
      }
      setFiles(prev => [...prev, { ...record, status: 'COMPLETED' }])
    } catch (err) {
      console.error('Upload failed', err)
    } finally {
      setUploading(false)
    }
  }, [user])

  // ── Delete ────────────────────────────────────────────────────────
  const handleDelete = useCallback(() => {
    if (!selectedFile?.id) return
    setModal('confirm-delete')
  }, [selectedFile])

  const confirmDelete = useCallback(async () => {
    if (!selectedFile?.id) return
    setModal(null)
    try {
      await deleteFile(selectedFile.id)
      setFiles(prev => prev.filter(f => f.id !== selectedFile.id))
      setSelectedIndex(null)
    } catch (err) {
      if (axios.isAxiosError(err)) {
        if (err.response?.status === 403) {
          alert('You can only delete your own files.')
        } else if (err.response?.status === 404) {
          setFiles(prev => prev.filter(f => f.id !== selectedFile.id))
          setSelectedIndex(null)
        } else {
          alert('Delete failed. Please try again.')
        }
      }
    }
  }, [selectedFile])

  // ── Share ─────────────────────────────────────────────────────────
  const handleShare = useCallback(() => {
    if (!selectedFile?.id) return
    setModal('share')
  }, [selectedFile])

  const handleGenerateLink = useCallback(async (): Promise<FileRecord> => {
    const updated = await updateVisibility(selectedFile!.id!, 'PUBLIC')
    setFiles(prev => prev.map(f => f.id === updated.id ? updated : f))
    return updated
  }, [selectedFile])

  const handleRevokeLink = useCallback(async (): Promise<FileRecord> => {
    const updated = await updateVisibility(selectedFile!.id!, 'PRIVATE')
    setFiles(prev => prev.map(f => f.id === updated.id ? updated : f))
    return updated
  }, [selectedFile])

  const handleEmailShare = useCallback(async (email: string): Promise<void> => {
    await shareFileWithUser(selectedFile!.id!, email)
  }, [selectedFile])

  // ── Keyboard shortcuts + grid navigation ─────────────────────────
  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      const tag = (e.target as HTMLElement).tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
      if (modal === 'viewer' && e.key.toLowerCase() === 'b') {
        e.preventDefault()
        setActiveKey('b')
        setTimeout(() => setActiveKey(null), 150)
        setModal(null)
        return
      }
      if (modal !== null) return

      // Arrow key grid navigation
      if (['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(e.key)) {
        e.preventDefault()
        setSelectedIndex(prev => {
          const cur  = prev ?? 0
          const size = gridSizeRef.current
          switch (e.key) {
            case 'ArrowRight': return Math.min(size - 1, cur + 1)
            case 'ArrowLeft':  return Math.max(0, cur - 1)
            case 'ArrowUp':    return Math.max(0, cur - COLS)
            case 'ArrowDown':  return Math.min(size - 1, cur + COLS)
            default:           return prev
          }
        })
        return
      }

      const key = e.key.toLowerCase()
      if (!['a', 'b', 'x', 'y', 'z'].includes(key)) return

      e.preventDefault()
      setActiveKey(key)
      setTimeout(() => setActiveKey(null), 150)

      switch (key) {
        case 'a': if (isLoggedIn && hasSelection) setModal('viewer'); break
        case 'b': setSelectedIndex(null); break
        case 'x': if (isLoggedIn && isVerified) triggerUpload(); break
        case 'y': if (isLoggedIn && isVerified && hasSelection) handleDelete(); break
        case 'z': if (isLoggedIn && isVerified && hasSelection) handleShare(); break
      }
    }

    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [modal, isLoggedIn, isVerified, hasSelection, triggerUpload, handleDelete, handleShare])

  // ── Early returns (after all hooks) ──────────────────────────────
  if (VERIFY_TOKEN) return <VerifyEmailPage token={VERIFY_TOKEN} />
  if (PUBLIC_VIEW_TOKEN) return <PublicViewer token={PUBLIC_VIEW_TOKEN} />
  if (!isLoading && isLoggedIn && !isVerified) return <EmailGate />

  return (
    <div className={s.app}>
      <TopBar
        activeTab={activeTab}
        onTabChange={handleTabChange}
        user={user}
        isLoggedIn={isLoggedIn}
        onLogin={() => setModal('login')}
        onRegister={() => setModal('register')}
        onLogout={logout}
        onProfile={() => setModal('profile')}
      />

      <div className={s.main}>
        <FileDataPanel file={selectedSlot} isLoggedIn={isLoggedIn} />
        <BoxArea
          slots={displaySlots}
          selected={selectedIndex}
          onSelect={handleSelect}
          isLoggedIn={isLoggedIn}
          onUpload={triggerUpload}
        />
      </div>

      <ControlsBar
        isLoggedIn={isLoggedIn}

        hasSelection={hasSelection}
        activeKey={activeKey}
        onOpen={() => setModal('viewer')}
        onUpload={triggerUpload}
        onDelete={handleDelete}
        onShare={handleShare}
      />

      <input
        ref={uploadRef}
        type="file"
        style={{ display: 'none' }}
        onChange={handleFileChange}
        disabled={uploading}
      />

      {modal === 'login'    && <LoginModal    onClose={closeModal} onSwitchToRegister={() => setModal('register')} onSuccess={closeModal} />}
      {modal === 'register' && <RegisterModal onClose={closeModal} onSwitchToLogin={() => setModal('login')}      onSuccess={closeModal} />}
      {modal === 'profile'  && <ProfileModal  onClose={closeModal} />}
      {modal === 'viewer'   && selectedFile && <FileViewerModal file={selectedFile} onClose={closeModal} />}
      {modal === 'share'    && selectedFile && (
        <ShareModal
          file={selectedFile}
          onClose={closeModal}
          onGenerateLink={handleGenerateLink}
          onRevokeLink={handleRevokeLink}
          onEmailShare={handleEmailShare}
          onFileUpdate={updated => setFiles(prev => prev.map(f => f.id === updated.id ? updated : f))}
        />
      )}
      {modal === 'confirm-delete' && selectedSlot && (
        <Modal title="Delete File" onClose={closeModal} maxWidth={340}>
          <div style={{ textAlign: 'center', display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div style={{ fontSize: 36 }}>{selectedSlot.emoji}</div>
            <p style={{ margin: 0, fontSize: 13, color: 'var(--text-white)', lineHeight: 1.5 }}>
              Delete <strong>{selectedSlot.name}</strong>?<br />
              <span style={{ color: 'var(--text-dim)', fontSize: 11 }}>This cannot be undone.</span>
            </p>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'center' }}>
              <GcButton variant="b" label="Cancel" onClick={closeModal} />
              <GcButton variant="y" label="Delete" onClick={confirmDelete} />
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}
