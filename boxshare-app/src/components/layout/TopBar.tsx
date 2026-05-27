import GcButton from '../button/GcButton'
import s from './TopBar.module.css'

interface Props {
  onLogin: () => void
  onRegister: () => void
}

export default function TopBar({ onLogin, onRegister }: Props) {
  return (
    <div className={s.topbar}>
      <div className={s.tabs}>
        <div className={`${s.tab} ${s.active}`}>Party FILES</div>
        <div className={s.tab}>BOX FILES</div>
      </div>
      <div className={s.actions}>
        <GcButton variant="b" label="Log In"   onClick={onLogin} />
        <GcButton variant="a" label="Register" onClick={onRegister} />
      </div>
    </div>
  )
}
