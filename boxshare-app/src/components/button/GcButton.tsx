import s from './GcButton.module.css'

type Variant = 'a' | 'b' | 'x' | 'y'

interface Props {
  variant: Variant
  label: string
  large?: boolean
  onClick?: () => void
  type?: 'button' | 'submit'
  disabled?: boolean
}

export default function GcButton({ variant, label, large, onClick, type = 'button', disabled }: Props) {
  return (
    <button
      type={type}
      className={[s.btn, s[variant], large ? s.lg : ''].join(' ')}
      onClick={onClick}
      disabled={disabled}
    >
      <span className={s.dot} />
      {label}
    </button>
  )
}
