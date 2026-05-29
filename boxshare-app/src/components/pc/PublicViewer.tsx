import { useEffect, useState } from 'react'
import { getPublicFile, type FileRecord } from '../../api/files'
import { formatSize, emojiForMime } from '../../utils/fileUtils'
import s from './PublicViewer.module.css'

interface Props {
  token: string
}

function ViewerContent({ file, url }: { file: FileRecord; url: string }) {
  const mime = file.mimeType

  if (mime.startsWith('image/'))
    return <img className={s.image} src={url} alt={file.name} />

  if (mime.startsWith('video/'))
    return <video className={s.media} src={url} controls />

  if (mime.startsWith('audio/'))
    return (
      <div className={s.audioWrap}>
        <span className={s.audioIcon}>🎵</span>
        <audio className={s.audio} src={url} controls />
      </div>
    )

  if (mime === 'application/pdf')
    return <iframe className={s.pdf} src={url} title={file.name} />

  if (mime.startsWith('text/'))
    return <TextViewer url={url} />

  return (
    <div className={s.fallback}>
      <span className={s.fallbackIcon}>{emojiForMime(mime)}</span>
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
    fetch(url).then(r => r.text()).then(setText).catch(() => setText('Could not load file content.'))
  }, [url])

  if (text === null) return <p className={s.status}>Loading…</p>
  return <pre className={s.textContent}>{text}</pre>
}

export default function PublicViewer({ token }: Props) {
  const [file,  setFile]  = useState<FileRecord | null>(null)
  const [url,   setUrl]   = useState<string | null>(null)
  const [state, setState] = useState<'loading' | 'ready' | 'error'>('loading')

  useEffect(() => {
    getPublicFile(token)
      .then(record => {
        if (record.presignedUrl) {
          setFile(record)
          setUrl(record.presignedUrl)
          setState('ready')
        } else {
          setState('error')
        }
      })
      .catch(() => setState('error'))
  }, [token])

  return (
    <div className={s.page}>
      <div className={s.header}>
        <span className={s.logo}>BoxShare</span>
        <a className={s.homeBtn} href="/">Go to BoxShare</a>
      </div>

      <div className={s.card}>
        {state === 'loading' && <p className={s.status}>Loading…</p>}
        {state === 'error'   && (
          <div className={s.errorWrap}>
            <span className={s.errorIcon}>🔒</span>
            <p className={s.errorMsg}>This file is not available or the link has expired.</p>
            <a className={s.homeBtn} href="/">Go to BoxShare</a>
          </div>
        )}
        {state === 'ready' && file && url && (
          <>
            <div className={s.meta}>
              <span className={s.fileIcon}>{emojiForMime(file.mimeType)}</span>
              <div className={s.fileInfo}>
                <span className={s.fileName}>{file.name}</span>
                <span className={s.fileSub}>{formatSize(file.size)} · {file.mimeType}</span>
              </div>
              <a className={s.downloadBtn} href={url} download={file.name} target="_blank" rel="noreferrer">
                Download
              </a>
            </div>
            <div className={s.viewer}>
              <ViewerContent file={file} url={url} />
            </div>
          </>
        )}
      </div>
    </div>
  )
}
