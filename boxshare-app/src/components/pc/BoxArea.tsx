import type { FileSlot } from '../../types/file'
import SlotGrid from './SlotGrid'
import s from './BoxArea.module.css'

interface Props {
  slots: (FileSlot | null)[]
  selected: number
  onSelect: (index: number) => void
  boxName?: string
  boxNumber?: number
  totalBoxes?: number
}

export default function BoxArea({
  slots, selected, onSelect,
  boxName = 'BOX 1', boxNumber = 1, totalBoxes = 8,
}: Props) {
  const filledCount = slots.filter(Boolean).length

  return (
    <div className={s.area}>
      <div className={s.nav}>
        <div className={s.navBtn}>◀</div>
        <div className={s.navLabel}>{boxName}</div>
        <div className={s.navBtn}>▶</div>
      </div>

      <SlotGrid slots={slots} selected={selected} onSelect={onSelect} />

      <div className={s.stat}>
        <div>Files: <span>{filledCount}</span> / {slots.length}</div>
        <div>Box: <span>{boxNumber}</span> of {totalBoxes}</div>
      </div>
    </div>
  )
}
