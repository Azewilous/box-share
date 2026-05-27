import s from './ControlsBar.module.css'

interface Props {
  isLoggedIn: boolean
  isAdmin: boolean
  hasSelection: boolean
  activeKey: string | null
  onOpen: () => void
  onUpload: () => void
  onDelete: () => void
  onShare: () => void
}

export default function ControlsBar({ isLoggedIn, isAdmin, hasSelection, activeKey, onOpen, onUpload, onDelete, onShare }: Props) {
  const controls = [
    { id: 'a', label: 'Open',    onClick: onOpen,     active: isLoggedIn && hasSelection },
    { id: 'b', label: 'Back',    onClick: undefined,  active: true },
    { id: 'x', label: 'Upload',  onClick: onUpload,   active: isLoggedIn },
    { id: 'y', label: 'Delete',  onClick: onDelete,   active: isLoggedIn && hasSelection },
    { id: 'z', label: 'Share',   onClick: onShare,    active: isLoggedIn && hasSelection },
  ] as const

  return (
    <div className={s.bar}>
      {controls.map(({ id, label, onClick, active }) => (
        <button
          key={id}
          className={[
            s.ctrl,
            s[id],
            !active          ? s.inactive : '',
            activeKey === id ? s.pressed  : '',
          ].join(' ')}
          onClick={active && onClick ? onClick : undefined}
          disabled={!active || !onClick}
          aria-label={label}
        >
          <div className={s.dot}>{id.toUpperCase()}</div>
          {label}
        </button>
      ))}
    </div>
  )
}
