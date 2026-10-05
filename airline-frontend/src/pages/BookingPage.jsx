import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api, savedPnrs } from '../api.js'
import ErrorBox from '../components/ErrorBox.jsx'
import BookingCard from '../components/BookingCard.jsx'

export default function BookingPage() {
  const { pnr: pnrParam } = useParams()
  const navigate = useNavigate()
  const [input, setInput] = useState(pnrParam || '')
  const [booking, setBooking] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const [cancelling, setCancelling] = useState(false)
  const [confirmCancel, setConfirmCancel] = useState(false)
  const [recent, setRecent] = useState(savedPnrs())

  const load = useCallback(async (pnr) => {
    setError(null); setBooking(null); setConfirmCancel(false); setLoading(true)
    try {
      setBooking(await api.getBooking(pnr.trim()))
    } catch (err) {
      setError(err)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    setInput(pnrParam || '')
    if (pnrParam) load(pnrParam)
    else { setBooking(null); setError(null) }
    setRecent(savedPnrs())
  }, [pnrParam, load])

  function submit(e) {
    e.preventDefault()
    if (input.trim()) navigate(`/bookings/${input.trim()}`)
  }

  async function cancel() {
    setError(null); setCancelling(true)
    try {
      setBooking(await api.cancelBooking(booking.pnr))
      setConfirmCancel(false)
    } catch (err) {
      setError(err)
    } finally {
      setCancelling(false)
    }
  }

  return (
    <>
      <section className="hero">
        <h1>My bookings</h1>
        <p className="muted">Look up a booking by its reference to view or cancel it.</p>
      </section>

      <form className="card lookup" onSubmit={submit}>
        <label>Booking reference (PNR)
          <input value={input} onChange={(e) => setInput(e.target.value)} placeholder="e.g. 3f2b8c1e-…" required />
        </label>
        <button className="btn btn-primary" disabled={loading}>{loading ? 'Looking up…' : 'Find booking'}</button>
      </form>

      {!pnrParam && recent.length > 0 && (
        <div className="card">
          <h2>Recent on this device</h2>
          <ul className="recent">
            {recent.map((p) => (
              <li key={p}><button className="link" onClick={() => navigate(`/bookings/${p}`)}><code>{p}</code></button></li>
            ))}
          </ul>
        </div>
      )}

      <ErrorBox error={error} />

      {booking && (
        <>
          <BookingCard booking={booking} onCancel={() => setConfirmCancel(true)} cancelling={cancelling} />
          {confirmCancel && booking.status !== 'CANCELLED' && (
            <div className="alert alert-warn" role="alertdialog">
              <strong>Cancel this booking?</strong> Seats {booking.seats.join(', ')} will be released immediately.
              <div className="actions">
                <button className="btn btn-danger" onClick={cancel} disabled={cancelling}>{cancelling ? 'Cancelling…' : 'Yes, cancel booking'}</button>
                <button className="btn btn-ghost" onClick={() => setConfirmCancel(false)} disabled={cancelling}>Keep booking</button>
              </div>
            </div>
          )}
          {booking.status === 'CANCELLED' && <div className="alert alert-ok">This booking is cancelled and its seats are available again.</div>}
        </>
      )}
    </>
  )
}
