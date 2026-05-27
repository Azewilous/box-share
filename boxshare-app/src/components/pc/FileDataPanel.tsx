import type { FileSlot } from '../../types/file'
import s from './FileDataPanel.module.css'

interface Props {
  file: FileSlot | null
  isLoggedIn: boolean
}

export default function FileDataPanel({ file, isLoggedIn }: Props) {
  const isEmpty = !file

  return (
    <div className={s.panel}>
      <div className={s.label}>File Data</div>
      <div className={s.preview}>
        <div className={s.icon}>{isEmpty ? '📂' : file.emoji}</div>
        <div className={s.name}>{isEmpty ? 'Empty slot' : file.name}</div>
        {!isEmpty && (
          <div className={s.meta}>
            <strong>Type</strong>
            {file.type}
            <strong style={{ marginTop: 8 }}>Size</strong>
            {file.size}
            {isLoggedIn && (
              <>
                <strong style={{ marginTop: 8 }}>Visibility</strong>
                <span className={file.visibility === 'PUBLIC' ? s.badgePublic : s.badgePrivate}>
                  {file.visibility}
                </span>
              </>
            )}
          </div>
        )}
      </div>
      <div className={s.divider} />
      <div className={s.hint}>
        {isLoggedIn
          ? isEmpty ? 'Select a file or press X to upload' : 'Press Z to share · Y to delete'
          : 'Sign in to upload and manage your files'}
      </div>
    </div>
  )
}
