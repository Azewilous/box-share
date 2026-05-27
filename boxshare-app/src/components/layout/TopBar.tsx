import GcButton from '../button/GcButton'
import s from './TopBar.module.css'
import type { AuthUser } from '../../context/AuthContext'

type Tab = 'party' | 'box'

interface Props {
  activeTab: Tab
  onTabChange: (tab: Tab) => void
  user: AuthUser | null
  isLoggedIn: boolean
  onLogin: () => void
  onRegister: () => void
  onLogout: () => void
  onProfile: () => void
}

export default function TopBar({
  activeTab, onTabChange, user, isLoggedIn, onLogin, onRegister, onLogout, onProfile,
}: Props) {
  return (
    <div className={s.topbar}>
      <div className={s.tabs}>
        <div
          className={`${s.tab} ${activeTab === 'party' ? s.active : ''}`}
          onClick={() => onTabChange('party')}
        >
          Party FILES
        </div>
        <div
          className={`${s.tab} ${activeTab === 'box' ? s.active : ''}`}
          onClick={() => onTabChange('box')}
        >
          BOX FILES
        </div>
      </div>

      <div className={s.actions}>
        {isLoggedIn ? (
          <>
            <button className={s.username} onClick={onProfile} title="Edit profile">
              {user?.email}
            </button>
            <GcButton variant="b" label="Log Out" onClick={onLogout} />
          </>
        ) : (
          <>
            <GcButton variant="b" label="Log In"   onClick={onLogin} />
            <GcButton variant="a" label="Register" onClick={onRegister} />
          </>
        )}
      </div>
    </div>
  )
}
