import s from './GcButton.module.css'

type Variant = 'a' | 'b' | 'x' | 'y'

interface Props {
  variant: Variant
  label: string
  large?: boolean
  onClick?: () => void
  type?: 'button' | 'submit'
}

export default function GcButton({ variant, label, large, onClick, type = 'button' }: Props) {
  return (
    <button
      type={type}
      className={[s.btn, s[variant], large ? s.lg : ''].join(' ')}
      onClick={onClick}
    >
      <span className={s.dot} />
      {label}
    </button>
  )
}
