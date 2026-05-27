import type { FileSlot } from '../../types/file'
import SlotGrid from './SlotGrid'
import s from './BoxArea.module.css'

interface Props {
  slots: (FileSlot | null)[]
  selected: number | null
  onSelect: (index: number) => void
  boxName?: string
  boxNumber?: number
  totalBoxes?: number
  isLoggedIn?: boolean
  onUpload?: () => void
}

export default function BoxArea({
  slots, selected, onSelect,
  boxName = 'BOX 1', boxNumber = 1, totalBoxes = 8,
  isLoggedIn = false, onUpload,
}: Props) {
  const filledCount = slots.filter(Boolean).length
  const isEmpty = isLoggedIn && filledCount === 0

  return (
    <div className={s.area}>
      <div className={s.nav}>
        <div className={s.navBtn}>◀</div>
        <div className={s.navLabel}>{boxName}</div>
        <div className={s.navBtn}>▶</div>
      </div>

      <div className={s.gridWrap}>
        <SlotGrid slots={slots} selected={selected} onSelect={onSelect} />
        {isEmpty && (
          <div className={s.emptyOverlay}>
            <span className={s.emptyIcon}>📭</span>
            <p className={s.emptyText}>No files yet</p>
            <button className={s.emptyBtn} onClick={onUpload}>Press X to upload</button>
          </div>
        )}
      </div>

      <div className={s.stat}>
        <div>Files: <span>{filledCount}</span> / {slots.length}</div>
        <div>Box: <span>{boxNumber}</span> of {totalBoxes}</div>
      </div>
    </div>
  )
}
