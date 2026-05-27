import { useEffect, useState } from 'react'
import Modal from '../modal/Modal'
import { getFileForView, type FileRecord } from '../../api/files'
import { formatSize } from '../../utils/fileUtils'
import s from './FileViewerModal.module.css'

interface Props {
  file: FileRecord
  onClose: () => void
}

type ViewState = 'loading' | 'ready' | 'error'

function ViewerContent({ file, url }: { file: FileRecord; url: string }) {
  const mime = file.mimeType

  if (mime.startsWith('image/')) {
    return <img className={s.image} src={url} alt={file.name} />
  }

  if (mime.startsWith('video/')) {
    return (
      <video className={s.media} src={url} controls autoPlay={false} />
    )
  }

  if (mime.startsWith('audio/')) {
    return (
      <div className={s.audioWrap}>
        <span className={s.audioIcon}>🎵</span>
        <audio className={s.audio} src={url} controls />
      </div>
    )
  }

  if (mime === 'application/pdf') {
    return <iframe className={s.pdf} src={url} title={file.name} />
  }

  if (mime.startsWith('text/')) {
    return <TextViewer url={url} />
  }

  return (
    <div className={s.fallback}>
      <span className={s.fallbackIcon}>📁</span>
      <p className={s.fallbackMsg}>Preview not available for this file type.</p>
      <a className={s.downloadBtn} href={url} download={file.name} target="_blank" rel="noreferrer">
        Download File
      </a>
    </div>
  )
}

function TextViewer({ url }: { url: string }) {
  const [text, setText] = useState<string | null>(null)

  useEffect(() => {
    fetch(url)
      .then(r => r.text())
      .then(setText)
      .catch(() => setText('Could not load file content.'))
  }, [url])

  if (text === null) return <p className={s.status}>Loading…</p>
  return <pre className={s.textContent}>{text}</pre>
}

export default function FileViewerModal({ file, onClose }: Props) {
  const [state, setState] = useState<ViewState>('loading')
  const [viewUrl, setViewUrl] = useState<string | null>(null)

  useEffect(() => {
    getFileForView(file.name)
      .then(record => {
        if (record.presignedUrl) {
          setViewUrl(record.presignedUrl)
          setState('ready')
        } else {
          setState('error')
        }
      })
      .catch(() => setState('error'))
  }, [file.name])

  const title = `— ${file.name} —`

  return (
    <Modal title={title} onClose={onClose} maxWidth={760}>
      <div className={s.meta}>
        <span className={s.metaItem}>{formatSize(file.size)}</span>
        <span className={s.metaDot}>·</span>
        <span className={s.metaItem}>{file.mimeType}</span>
        <span className={s.metaDot}>·</span>
        <span className={s.metaItem}>{file.visibility}</span>
      </div>

      <div className={s.viewer}>
        {state === 'loading' && <p className={s.status}>Loading…</p>}
        {state === 'error'   && <p className={s.statusError}>Could not load file preview.</p>}
        {state === 'ready' && viewUrl && <ViewerContent file={file} url={viewUrl} />}
      </div>

      {state === 'ready' && viewUrl && (
        <div className={s.footer}>
          <a className={s.downloadBtn} href={viewUrl} download={file.name} target="_blank" rel="noreferrer">
            Download
          </a>
        </div>
      )}
    </Modal>
  )
}
