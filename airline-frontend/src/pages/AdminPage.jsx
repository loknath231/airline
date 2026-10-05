import { useCallback, useEffect, useState } from 'react'
import { AIRCRAFT, api } from '../api.js'
import ErrorBox from '../components/ErrorBox.jsx'

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const short = (d) => d.slice(0, 3)
const blank = {
  flightNumber: '', sourceAirport: 'DXB', destinationAirport: 'LHR',
  departureTime: '09:30', arrivalTime: '13:45', arrivalDayOffset: 0, aircraftId: 1,
  daysOfOperation: ['MONDAY', 'WEDNESDAY', 'FRIDAY'],
}

export default function AdminPage() {
  const [airports, setAirports] = useState([])
  const [schedules, setSchedules] = useState([])
  const [form, setForm] = useState(blank)
  const [error, setError] = useState(null)
  const [created, setCreated] = useState(null)
  const [busy, setBusy] = useState(false)

  const reload = useCallback(() => api.listSchedules().then(setSchedules).catch(setError), [])
  useEffect(() => {
    api.airports().then(setAirports).catch(setError)
    reload()
  }, [reload])

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))
  const toggleDay = (d) =>
    setForm((f) => ({
      ...f,
      daysOfOperation: f.daysOfOperation.includes(d) ? f.daysOfOperation.filter((x) => x !== d) : [...f.daysOfOperation, d],
    }))

  async function submit(e) {
    e.preventDefault()
    setError(null); setCreated(null); setBusy(true)
    try {
      const res = await api.createSchedule({
        ...form,
        flightNumber: form.flightNumber.trim().toUpperCase(),
        arrivalDayOffset: Number(form.arrivalDayOffset),
        aircraftId: Number(form.aircraftId),
      })
      setCreated(res)
      setForm((f) => ({ ...f, flightNumber: '' }))
      reload()
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <section className="hero">
        <h1>Back office</h1>
        <p className="muted">Define flight schedules. Each schedule generates bookable flights for the next 365 days.</p>
      </section>

      <div className="admin-grid">
        <form className="card admin-form" onSubmit={submit}>
          <h2>New schedule</h2>
          <label>Flight number
            <input value={form.flightNumber} onChange={set('flightNumber')} placeholder="XY101" pattern="[A-Za-z0-9]{3,8}" title="3-8 letters or digits" required />
          </label>
          <div className="two">
            <label>Source
              <select value={form.sourceAirport} onChange={set('sourceAirport')}>
                {airports.map((a) => <option key={a.code} value={a.code}>{a.city} ({a.code})</option>)}
              </select>
            </label>
            <label>Destination
              <select value={form.destinationAirport} onChange={set('destinationAirport')}>
                {airports.map((a) => <option key={a.code} value={a.code}>{a.city} ({a.code})</option>)}
              </select>
            </label>
          </div>
          <div className="three">
            <label>Departure (UTC)
              <input type="time" value={form.departureTime} onChange={set('departureTime')} required />
            </label>
            <label>Arrival (UTC)
              <input type="time" value={form.arrivalTime} onChange={set('arrivalTime')} required />
            </label>
            <label>Arrives
              <select value={form.arrivalDayOffset} onChange={set('arrivalDayOffset')}>
                <option value={0}>same day</option>
                <option value={1}>+1 day</option>
                <option value={2}>+2 days</option>
                <option value={3}>+3 days</option>
              </select>
            </label>
          </div>
          <label>Aircraft
            <select value={form.aircraftId} onChange={set('aircraftId')}>
              {AIRCRAFT.map((a) => <option key={a.id} value={a.id}>{a.label}</option>)}
            </select>
          </label>
          <fieldset>
            <legend>Days of operation</legend>
            <div className="days">
              {DAYS.map((d) => (
                <label key={d} className={`day ${form.daysOfOperation.includes(d) ? 'on' : ''}`}>
                  <input type="checkbox" checked={form.daysOfOperation.includes(d)} onChange={() => toggleDay(d)} />
                  {short(d)}
                </label>
              ))}
            </div>
          </fieldset>
          <ErrorBox error={error} />
          {created && (
            <div className="alert alert-ok">
              Schedule <strong>{created.flightNumber}</strong> created — {created.instancesGenerated} flights generated.
            </div>
          )}
          <button className="btn btn-primary btn-block" disabled={busy || form.daysOfOperation.length === 0}>
            {busy ? 'Creating…' : 'Create schedule'}
          </button>
        </form>

        <div className="card">
          <div className="row-between"><h2>Schedules ({schedules.length})</h2><button className="btn btn-ghost" onClick={reload}>↻ Refresh</button></div>
          <div className="table-scroll">
            <table className="table">
              <thead><tr><th>Flight</th><th>Route</th><th>Dep</th><th>Arr</th><th>Aircraft</th><th>Days</th></tr></thead>
              <tbody>
                {schedules.map((s) => (
                  <tr key={s.id}>
                    <td><strong>{s.flightNumber}</strong></td>
                    <td>{s.sourceAirport} → {s.destinationAirport}</td>
                    <td>{s.departureTime.slice(0, 5)}</td>
                    <td>{s.arrivalTime.slice(0, 5)}{s.arrivalDayOffset > 0 && <sup> +{s.arrivalDayOffset}</sup>}</td>
                    <td>{s.aircraftType}</td>
                    <td>{s.daysOfOperation.length === 7 ? 'Daily' : s.daysOfOperation.map(short).join(' ')}</td>
                  </tr>
                ))}
                {schedules.length === 0 && <tr><td colSpan="6" className="muted">No schedules yet.</td></tr>}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </>
  )
}
