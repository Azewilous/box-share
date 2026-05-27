import type { FileSlot } from '../../types/file'
import s from './FileDataPanel.module.css'

interface Props {
  file: FileSlot | null
}

const EMPTY: FileSlot = { emoji: '📂', name: 'Empty slot', type: '—', size: '—' }

export default function FileDataPanel({ file }: Props) {
  const display = file ?? EMPTY

  return (
    <div className={s.panel}>
      <div className={s.label}>File Data</div>
      <div className={s.preview}>
        <div className={s.icon}>{display.emoji}</div>
        <div className={s.name}>{display.name}</div>
        <div className={s.meta}>
          <strong>Type</strong>
          {display.type}
          <strong style={{ marginTop: 8 }}>Size</strong>
          {display.size}
        </div>
      </div>
      <div className={s.divider} />
      <div className={s.hint}>Sign in to upload and manage your files</div>
    </div>
  )
}
