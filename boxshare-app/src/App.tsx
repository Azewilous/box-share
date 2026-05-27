import { useState } from 'react'
import mockSlots from './data/mockSlots'
import TopBar from './components/layout/TopBar'
import ControlsBar from './components/layout/ControlsBar'
import FileDataPanel from './components/pc/FileDataPanel'
import BoxArea from './components/pc/BoxArea'
import LoginModal from './components/auth/LoginModal'
import RegisterModal from './components/auth/RegisterModal'
import s from './App.module.css'

type Modal = 'login' | 'register' | null

export default function App() {
  const [selected, setSelected] = useState(0)
  const [modal, setModal] = useState<Modal>(null)

  return (
    <div className={s.app}>
      <TopBar
        onLogin={() => setModal('login')}
        onRegister={() => setModal('register')}
      />

      <div className={s.main}>
        <FileDataPanel file={mockSlots[selected] ?? null} />
        <BoxArea
          slots={mockSlots}
          selected={selected}
          onSelect={setSelected}
        />
      </div>

      <ControlsBar />

      {modal === 'login' && (
        <LoginModal
          onClose={() => setModal(null)}
          onSwitchToRegister={() => setModal('register')}
        />
      )}
      {modal === 'register' && (
        <RegisterModal
          onClose={() => setModal(null)}
          onSwitchToLogin={() => setModal('login')}
        />
      )}
    </div>
  )
}
