import type { FileSlot } from '../../types/file'
import Slot from './Slot'
import s from './SlotGrid.module.css'

interface Props {
  slots: (FileSlot | null)[]
  selected: number
  onSelect: (index: number) => void
}

export default function SlotGrid({ slots, selected, onSelect }: Props) {
  return (
    <div className={s.wallpaper}>
      <div className={s.grid}>
        {slots.map((slot, i) => (
          <Slot
            key={i}
            slot={slot}
            selected={selected === i}
            onClick={() => onSelect(i)}
          />
        ))}
      </div>
      <div className={s.watermark}>BoxShare</div>
    </div>
  )
}
