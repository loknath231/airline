import { useMemo } from 'react'

// Renders the aircraft seat map: rows x letters with an aisle in the middle.
export default function SeatMap({ seats, selected, onToggle, max = 9 }) {
  const { rows, letters } = useMemo(() => {
    const byRow = new Map()
    const letterSet = new Set()
    for (const s of seats) {
      const m = /^(\d+)([A-Z])$/.exec(s.seat)
      if (!m) continue
      const row = Number(m[1])
      letterSet.add(m[2])
      if (!byRow.has(row)) byRow.set(row, new Map())
      byRow.get(row).set(m[2], s)
    }
    return { rows: [...byRow.entries()].sort((a, b) => a[0] - b[0]), letters: [...letterSet].sort() }
  }, [seats])

  const half = Math.ceil(letters.length / 2)
  const atMax = selected.length >= max

  return (
    <div className="seatmap" role="group" aria-label="Seat map">
      <div className="seat-row seat-head">
        <span className="row-no" />
        {letters.map((l, i) => (
          <span key={l} className={`seat-head-cell ${i === half ? 'after-aisle' : ''}`}>{l}</span>
        ))}
      </div>
      {rows.map(([row, map]) => (
        <div className="seat-row" key={row}>
          <span className="row-no">{row}</span>
          {letters.map((l, i) => {
            const s = map.get(l)
            const label = `${row}${l}`
            const booked = s?.status === 'BOOKED'
            const isSel = selected.includes(label)
            const disabled = booked || (!isSel && atMax)
            return (
              <button
                key={l}
                type="button"
                className={`seat ${booked ? 'booked' : ''} ${isSel ? 'selected' : ''} ${i === half ? 'after-aisle' : ''}`}
                disabled={disabled}
                aria-pressed={isSel}
                aria-label={`Seat ${label} ${booked ? 'booked' : isSel ? 'selected' : 'available'}`}
                onClick={() => onToggle(label)}
              >
                {booked ? '✕' : l}
              </button>
            )
          })}
        </div>
      ))}
    </div>
  )
}
