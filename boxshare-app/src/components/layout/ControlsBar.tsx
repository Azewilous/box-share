import s from './ControlsBar.module.css'

const CONTROLS = [
  { id: 'a', label: 'Select'  },
  { id: 'b', label: 'Back'    },
  { id: 'x', label: 'Upload'  },
  { id: 'y', label: 'Delete'  },
] as const

export default function ControlsBar() {
  return (
    <div className={s.bar}>
      {CONTROLS.map(({ id, label }) => (
        <div key={id} className={`${s.ctrl} ${s[id]}`}>
          <div className={s.dot}>{id.toUpperCase()}</div>
          {label}
        </div>
      ))}
    </div>
  )
}
