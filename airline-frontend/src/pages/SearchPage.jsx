import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, rememberPnr } from '../api.js'
import { durationLabel, fmtDate, fmtTime, todayUtc } from '../format.js'
import ErrorBox from '../components/ErrorBox.jsx'
import SeatMap from '../components/SeatMap.jsx'
import BookingCard from '../components/BookingCard.jsx'

export default function SearchPage() {
  const [airports, setAirports] = useState([])
  const [form, setForm] = useState({ source: 'DXB', destination: 'LHR', date: todayUtc(1) })
  const [results, setResults] = useState(null) // null = not searched yet
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState(null)

  const [flight, setFlight] = useState(null)   // chosen search result
  const [seatMap, setSeatMap] = useState(null)
  const [selected, setSelected] = useState([])
  const [names, setNames] = useState({})
  const [booking, setBooking] = useState(null) // confirmed booking
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api.airports().then(setAirports).catch(setError)
  }, [])

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))
  const swap = () => setForm((f) => ({ ...f, source: f.destination, destination: f.source }))

  async function search(e) {
    e?.preventDefault()
    setError(null); setFlight(null); setSeatMap(null); setBooking(null); setSearching(true)
    try {
      setResults(await api.searchFlights(form))
    } catch (err) {
      setResults(null); setError(err)
    } finally {
      setSearching(false)
    }
  }

  async function chooseFlight(f) {
    setError(null); setFlight(f); setSelected([]); setNames({}); setBooking(null)
    try {
      setSeatMap(await api.seatMap(f.flightInstanceId))
    } catch (err) {
      setError(err)
    }
  }

  async function refreshSeats() {
    if (!flight) return
    try { setSeatMap(await api.seatMap(flight.flightInstanceId)) } catch (err) { setError(err) }
  }

  const toggleSeat = (label) =>
    setSelected((s) => (s.includes(label) ? s.filter((x) => x !== label) : [...s, label]))

  async function book(e) {
    e.preventDefault()
    setError(null); setBusy(true)
    try {
      const b = await api.createBooking({
        flightInstanceId: flight.flightInstanceId,
        passengers: selected.map((seat) => ({ seat, name: (names[seat] || '').trim() })),
      })
      rememberPnr(b.pnr)
      setBooking(b)
      setSeatMap(null)
    } catch (err) {
      setError(err)
      if (err.status === 409) { // someone took a seat - refresh the map and drop stale selection
        const fresh = await api.seatMap(flight.flightInstanceId).catch(() => null)
        if (fresh) {
          setSeatMap(fresh)
          const free = new Set(fresh.seats.filter((s) => s.status === 'AVAILABLE').map((s) => s.seat))
          setSelected((sel) => sel.filter((s) => free.has(s)))
        }
      }
    } finally {
      setBusy(false)
    }
  }

  const allNamed = selected.length > 0 && selected.every((s) => (names[s] || '').trim())

  return (
    <>
      <section className="hero">
        <h1>Where to next?</h1>
        <p className="muted">Search a route, pick your seats and book in seconds.</p>
      </section>

      <form className="card search-form" onSubmit={search}>
        <label>From
          <select value={form.source} onChange={set('source')} required>
            {airports.map((a) => <option key={a.code} value={a.code}>{a.city} ({a.code})</option>)}
          </select>
        </label>
        <button type="button" className="btn btn-ghost swap" onClick={swap} aria-label="Swap airports" title="Swap airports">⇄</button>
        <label>To
          <select value={form.destination} onChange={set('destination')} required>
            {airports.map((a) => <option key={a.code} value={a.code}>{a.city} ({a.code})</option>)}
          </select>
        </label>
        <label>Date (UTC)
          <input type="date" value={form.date} min={todayUtc()} max={todayUtc(365)} onChange={set('date')} required />
        </label>
        <button className="btn btn-primary" disabled={searching || !airports.length}>
          {searching ? 'Searching…' : 'Search flights'}
        </button>
      </form>

      <ErrorBox error={error} />

      {results && !booking && (
        <section aria-live="polite">
          <h2>{results.length ? `${results.length} flight${results.length > 1 ? 's' : ''}` : 'No flights'} · {fmtDate(form.date)}</h2>
          {results.length === 0 && (
            <div className="card empty">
              No flights operate {form.source} → {form.destination} on this date. Try another day, or create a schedule in the <Link to="/admin">back office</Link>.
            </div>
          )}
          <ul className="flight-list">
            {results.map((f) => {
              const full = f.availableSeats === 0
              const active = flight?.flightInstanceId === f.flightInstanceId
              return (
                <li key={f.flightInstanceId} className={`card flight ${active ? 'active' : ''}`}>
                  <div className="flight-no">{f.flightNumber}</div>
                  <div className="flight-times">
                    <div><strong>{fmtTime(f.departureTime)}</strong><span className="muted small">{f.sourceAirport}</span></div>
                    <div className="line"><span>{durationLabel(f.departureTime, f.arrivalTime)}</span></div>
                    <div><strong>{fmtTime(f.arrivalTime)}</strong><span className="muted small">{f.destinationAirport}</span></div>
                  </div>
                  <div className="flight-seats">
                    <span className={`badge ${full ? 'badge-red' : f.availableSeats < 20 ? 'badge-amber' : 'badge-green'}`}>
                      {full ? 'Sold out' : `${f.availableSeats} of ${f.totalSeats} seats left`}
                    </span>
                  </div>
                  <button className="btn btn-primary" disabled={full} onClick={() => chooseFlight(f)}>
                    {active ? 'Selected' : 'Select seats'}
                  </button>
                </li>
              )
            })}
          </ul>
        </section>
      )}

      {flight && seatMap && !booking && (
        <section className="booking-grid">
          <div className="card">
            <div className="row-between">
              <h2>Choose seats · {seatMap.flightNumber} <span className="muted small">({seatMap.aircraftType})</span></h2>
              <button className="btn btn-ghost" onClick={refreshSeats}>↻ Refresh</button>
            </div>
            <div className="legend">
              <span><i className="seat sample" />Available</span>
              <span><i className="seat sample selected" />Selected</span>
              <span><i className="seat sample booked">✕</i>Booked</span>
              <span className="muted small">{seatMap.availableSeats} / {seatMap.totalSeats} free</span>
            </div>
            <div className="seatmap-scroll">
              <SeatMap seats={seatMap.seats} selected={selected} onToggle={toggleSeat} />
            </div>
          </div>

          <form className="card passenger-form" onSubmit={book}>
            <h2>Passengers</h2>
            {selected.length === 0 && <p className="muted">Pick up to 9 seats on the map.</p>}
            {selected.map((seat, i) => (
              <label key={seat}>
                Passenger {i + 1} · seat <span className="chip">{seat}</span>
                <input
                  value={names[seat] || ''}
                  maxLength={100}
                  placeholder="Full name"
                  onChange={(e) => setNames((n) => ({ ...n, [seat]: e.target.value }))}
                  required
                />
              </label>
            ))}
            <button className="btn btn-primary btn-block" disabled={!allNamed || busy}>
              {busy ? 'Booking…' : `Book ${selected.length || ''} seat${selected.length === 1 ? '' : 's'}`}
            </button>
          </form>
        </section>
      )}

      {booking && (
        <section>
          <div className="alert alert-ok">
            <strong>Booking confirmed!</strong> Keep your reference — you can look it up or cancel it under <Link to={`/bookings/${booking.pnr}`}>My bookings</Link>.
          </div>
          <BookingCard booking={booking} />
          <div className="actions">
            <button className="btn btn-primary" onClick={() => { setBooking(null); setFlight(null); search() }}>Book another flight</button>
          </div>
        </section>
      )}
    </>
  )
}
