import type { FileSlot } from '../../types/file'
import s from './SlotGrid.module.css'

interface Props {
  slot: FileSlot | null
  selected: boolean
  onClick: () => void
}

export default function Slot({ slot, selected, onClick }: Props) {
  const classes = [s.slot, !slot ? s.empty : '', selected ? s.selected : ''].join(' ')

  return (
    <div className={classes} title={slot?.name} onClick={onClick}>
      <span className={s.emoji}>{slot?.emoji}</span>
      {slot && (
        <>
          <span className={s.slotName}>{slot.name}</span>
          <span className={s.slotType}>{slot.type}</span>
        </>
      )}
    </div>
  )
}
