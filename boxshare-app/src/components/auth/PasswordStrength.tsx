import s from './PasswordStrength.module.css'

interface Props {
  password: string
}

interface Criterion {
  label: string
  color: string
  glow: string
  score: (p: string) => number
}

const CRITERIA: Criterion[] = [
  { label: 'LENGTH',  color: '#F8A060', glow: 'rgba(248,160,96,0.5)',  score: p => p.length >= 12 ? 1 : p.length >= 8 ? 0.65 : p.length >= 6 ? 0.35 : p.length > 0 ? 0.1 : 0 },
  { label: 'UPPER',   color: '#80A8F0', glow: 'rgba(128,168,240,0.5)', score: p => /[A-Z]/.test(p) ? 1 : 0 },
  { label: 'NUMBERS', color: '#F080B0', glow: 'rgba(240,128,176,0.5)', score: p => /[0-9]/.test(p) ? 1 : 0 },
  { label: 'SYMBOLS', color: '#80D090', glow: 'rgba(128,208,144,0.5)', score: p => /[^A-Za-z0-9]/.test(p) ? 1 : 0 },
  { label: 'LOWER',   color: '#E0D860', glow: 'rgba(224,216,96,0.5)',  score: p => /[a-z]/.test(p) ? 1 : 0 },
]

const CX = 140, CY = 128, R = 68, LABEL_R = 110, BLOB_R = R + 12
const ANGLES = Array.from({ length: 5 }, (_, i) => -Math.PI / 2 + (i * 2 * Math.PI) / 5)

function pt(angle: number, radius: number): [number, number] {
  return [CX + radius * Math.cos(angle), CY + radius * Math.sin(angle)]
}

function toPath(points: [number, number][]): string {
  return points.map(([x, y], i) => `${i === 0 ? 'M' : 'L'}${x.toFixed(2)},${y.toFixed(2)}`).join(' ') + ' Z'
}

const OUTLINE = toPath(ANGLES.map(a => pt(a, R)))
const RING1   = toPath(ANGLES.map(a => pt(a, R * 0.33)))
const RING2   = toPath(ANGLES.map(a => pt(a, R * 0.66)))

const STRENGTH: Array<{ label: string; color: string }> = [
  { label: '— NO PASSWORD —', color: '#7aaac8' },
  { label: 'WEAK',            color: '#e05050' },
  { label: 'FAIR',            color: '#e09030' },
  { label: 'GOOD',            color: '#d0d040' },
  { label: 'STRONG',          color: '#60d060' },
  { label: 'MAX',             color: '#80ff80' },
]

export default function PasswordStrength({ password }: Props) {
  const scores = CRITERIA.map(c => c.score(password))
  const total  = scores.reduce((a, b) => a + b, 0)
  const tier   = password.length === 0 ? 0 : Math.min(5, Math.max(1, Math.round(total)))
  const { label: strengthLabel, color: strengthColor } = STRENGTH[tier]

  const filledPts = scores.map((sc, i) => pt(ANGLES[i], sc > 0 ? Math.max(sc * R, 6) : 2))
  const filledPath = toPath(filledPts)

  return (
    <div className={s.wrap}>
      <svg viewBox="0 0 280 260" className={s.chart} aria-hidden="true">
        {/* pastel blob glows */}
        {CRITERIA.map((c, i) => {
          const [x, y] = pt(ANGLES[i], BLOB_R)
          return <circle key={i} cx={x} cy={y} r={32} fill={c.glow} />
        })}

        {/* grid rings */}
        <path d={RING1} fill="none" stroke="rgba(255,255,255,0.18)" strokeWidth="1.2" />
        <path d={RING2} fill="none" stroke="rgba(255,255,255,0.18)" strokeWidth="1.2" />

        {/* spokes */}
        {ANGLES.map((a, i) => {
          const [x, y] = pt(a, R)
          return <line key={i} x1={CX} y1={CY} x2={x} y2={y} stroke="rgba(255,255,255,0.25)" strokeWidth="1.2" />
        })}

        {/* outer pentagon */}
        <path d={OUTLINE} fill="none" stroke="rgba(255,255,255,0.5)" strokeWidth="2" />

        {/* filled strength polygon */}
        <path d={filledPath} fill="rgba(72,200,88,0.5)" stroke="#50d060" strokeWidth="2" strokeLinejoin="round" />

        {/* vertex dots for met criteria */}
        {scores.map((sc, i) => {
          if (sc === 0) return null
          const [x, y] = pt(ANGLES[i], Math.max(sc * R, 6))
          return <circle key={i} cx={x} cy={y} r={5} fill="#90ff90" />
        })}

        {/* axis labels */}
        {CRITERIA.map((c, i) => {
          const [x, y] = pt(ANGLES[i], LABEL_R)
          const active = scores[i] > 0
          return (
            <text
              key={i}
              x={x}
              y={y}
              textAnchor="middle"
              dominantBaseline="middle"
              className={s.axisLabel}
              fill={active ? c.color : 'rgba(255,255,255,0.3)'}
            >
              {c.label}
            </text>
          )
        })}
      </svg>

      <p className={s.strengthLabel} style={{ color: strengthColor }}>
        {strengthLabel}
      </p>
    </div>
  )
}
