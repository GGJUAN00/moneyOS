import type { ComponentType } from 'react'
import { useNow } from '../hooks/useNow'
import { capitalize, colombiaLongDate, colombiaTime } from '../lib/format'
import { displayName, initialsOf, type UserProfile } from '../lib/user'
import type { TxType } from '../lib/finance'

export interface NavItem<K extends string> {
  key: K
  label: string
  Icon: ComponentType<{ active: boolean }>
}

interface Props<K extends string> {
  items: NavItem<K>[]
  active: K
  user: UserProfile
  onSelect: (key: K) => void
  onAdd: (type: TxType) => void
  onOpenProfile: () => void
}

/** Left navigation for the desktop layout. */
export default function Sidebar<K extends string>({ items, active, user, onSelect, onAdd, onOpenProfile }: Props<K>) {
  const now = useNow(1000)
  return (
    <aside
      className="flex flex-col flex-shrink-0 h-full"
      style={{ width: 248, background: '#0A0A0B', borderRight: '1px solid rgba(255,255,255,0.06)' }}
    >
      {/* Brand — also the window drag area in the desktop app */}
      <div className="app-drag flex items-center gap-3 px-6 pt-8 pb-6">
        <div className="w-9 h-9 rounded-xl flex items-center justify-center metal-card">
          <svg width="18" height="18" viewBox="0 0 22 22" fill="none">
            <rect x="3" y="13" width="4" height="7" rx="1" fill="#F4F4F5" opacity="0.55" />
            <rect x="9" y="8" width="4" height="12" rx="1" fill="#F4F4F5" opacity="0.8" />
            <rect x="15" y="3" width="4" height="17" rx="1" fill="#F4F4F5" />
          </svg>
        </div>
        <span className="text-lg font-light" style={{ color: '#FFFFFF', letterSpacing: '-0.01em' }}>MoneyOS</span>
      </div>

      {/* Colombian date & time */}
      <div className="mx-4 mb-5 rounded-2xl px-4 py-3" style={{ background: '#111112', border: '1px solid rgba(255,255,255,0.06)' }}>
        <p className="text-2xl font-light font-mono-data" style={{ color: '#FFFFFF' }}>{colombiaTime(now)}</p>
        <p className="text-[11px] mt-0.5" style={{ color: '#8C8C93' }}>{capitalize(colombiaLongDate(now))}</p>
        <p className="text-[9px] mt-1" style={{ color: '#5E5E65', letterSpacing: '0.18em' }}>HORA DE COLOMBIA</p>
      </div>

      <nav className="px-3 space-y-1" aria-label="Secciones">
        {items.map(({ key, label, Icon }) => {
          const on = key === active
          return (
            <button
              key={key}
              onClick={() => onSelect(key)}
              aria-current={on ? 'page' : undefined}
              className="w-full flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition-all duration-200 btn-press"
              style={{
                background: on ? '#1A1A1C' : 'transparent',
                color: on ? '#FFFFFF' : '#8C8C93',
                border: on ? '1px solid rgba(255,255,255,0.08)' : '1px solid transparent',
              }}
            >
              <Icon active={on} />
              {label}
            </button>
          )
        })}
      </nav>

      <div className="px-4 mt-6 space-y-2">
        <button onClick={() => onAdd('ingreso')} className="w-full rounded-xl py-2.5 text-sm font-semibold btn-press" style={{ background: '#F4F4F5', color: '#050505' }}>
          ＋ Agregar dinero
        </button>
        <button onClick={() => onAdd('gasto')} className="w-full rounded-xl py-2.5 text-sm font-semibold btn-press" style={{ background: '#111112', color: '#F4F4F5', border: '1px solid rgba(255,255,255,0.14)' }}>
          − Transacción
        </button>
      </div>

      <button
        onClick={onOpenProfile}
        className="mt-auto m-4 flex items-center gap-3 rounded-2xl p-3 text-left btn-press"
        style={{ background: '#111112', border: '1px solid rgba(255,255,255,0.06)' }}
      >
        <span className="w-9 h-9 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0" style={{ background: 'linear-gradient(135deg, #F4F4F5 0%, #8C8C93 100%)', color: '#050505' }}>
          {initialsOf(user)}
        </span>
        <span className="min-w-0">
          <span className="block text-sm truncate" style={{ color: '#F4F4F5' }}>{displayName(user)}</span>
          <span className="block text-[11px]" style={{ color: '#8C8C93' }}>Perfil y seguridad</span>
        </span>
      </button>
    </aside>
  )
}
