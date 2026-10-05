import { fmtDate, fmtDateTime } from '../format.js'

export default function BookingCard({ booking, onCancel, cancelling }) {
  const cancelled = booking.status === 'CANCELLED'
  return (
    <article className={`card booking-card ${cancelled ? 'is-cancelled' : ''}`}>
      <header className="booking-head">
        <div>
          <div className="muted small">Booking reference (PNR)</div>
          <code className="pnr">{booking.pnr}</code>
        </div>
        <span className={`badge ${cancelled ? 'badge-red' : 'badge-green'}`}>{booking.status}</span>
      </header>
      <dl className="facts">
        <div><dt>Flight</dt><dd>{booking.flightNumber}</dd></div>
        <div><dt>Date</dt><dd>{fmtDate(booking.flightDate)}</dd></div>
        <div><dt>Departure</dt><dd>{fmtDateTime(booking.departureTime)}</dd></div>
        <div><dt>Passengers</dt><dd>{booking.passengerCount}</dd></div>
        <div><dt>Seats</dt><dd>{booking.seats.join(', ')}</dd></div>
        {booking.cancelledAt && <div><dt>Cancelled</dt><dd>{fmtDateTime(booking.cancelledAt)}</dd></div>}
      </dl>
      <table className="table">
        <thead><tr><th>Passenger</th><th>Seat</th></tr></thead>
        <tbody>
          {booking.passengers.map((p) => (
            <tr key={p.seat}><td>{p.name}</td><td><span className="chip">{p.seat}</span></td></tr>
          ))}
        </tbody>
      </table>
      {onCancel && !cancelled && (
        <footer className="actions">
          <button className="btn btn-danger" onClick={onCancel} disabled={cancelling}>
            {cancelling ? 'Cancelling…' : 'Cancel booking'}
          </button>
        </footer>
      )}
    </article>
  )
}
