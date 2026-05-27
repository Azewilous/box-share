import type { ReactNode } from 'react'
import s from './Modal.module.css'

interface Props {
  title: string
  onClose: () => void
  children: ReactNode
  maxWidth?: number
}

export default function Modal({ title, onClose, children, maxWidth = 400 }: Props) {
  return (
    <div className={s.backdrop} onClick={onClose}>
      <div
        className={s.panel}
        style={{ maxWidth }}
        onClick={e => e.stopPropagation()}
      >
        <div className={s.header}>{title}</div>
        <div className={s.body}>{children}</div>
      </div>
    </div>
  )
}
