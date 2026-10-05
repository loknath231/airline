// Tiny in-memory stand-in for the Spring Boot API so the UI can run without the backend:
//   npm run mock   (listens on :8080 - same port the Vite proxy targets)
import http from 'node:http'
import { randomUUID } from 'node:crypto'

const airports = [
  ['DXB', 'Dubai International Airport', 'Dubai', 'United Arab Emirates'], ['AUH', 'Zayed International Airport', 'Abu Dhabi', 'United Arab Emirates'],
  ['LHR', 'Heathrow Airport', 'London', 'United Kingdom'], ['JFK', 'John F. Kennedy International Airport', 'New York', 'United States'],
  ['CDG', 'Charles de Gaulle Airport', 'Paris', 'France'], ['DEL', 'Indira Gandhi International Airport', 'Delhi', 'India'],
  ['BOM', 'Chhatrapati Shivaji Maharaj International Airport', 'Mumbai', 'India'], ['SIN', 'Changi Airport', 'Singapore', 'Singapore'],
].map(([code, name, city, country]) => ({ code, name, city, country }))
const aircraft = { 1: { type: 'A320', rows: 30, letters: 'ABCDEF' }, 2: { type: 'B737', rows: 25, letters: 'ABCDEF' }, 3: { type: 'A330', rows: 40, letters: 'ABCDEFGH' } }
const DOW = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']
const schedules = []; const instances = []; const bookings = new Map()
const iso = (d, t, off = 0) => { const x = new Date(`${d}T${t}:00Z`); x.setUTCDate(x.getUTCDate() + off); return x.toISOString() }

function addSchedule(b) {
  const s = { id: schedules.length + 1, ...b, aircraftType: aircraft[b.aircraftId].type }
  schedules.push(s); let n = 0
  for (let i = 0; i <= 365; i++) {
    const d = new Date(); d.setUTCDate(d.getUTCDate() + i)
    if (!b.daysOfOperation.includes(DOW[d.getUTCDay()])) continue
    const date = d.toISOString().slice(0, 10)
    instances.push({ id: instances.length + 1, s, date, dep: iso(date, b.departureTime), arr: iso(date, b.arrivalTime, b.arrivalDayOffset), seats: new Map() }); n++
  }
  return { ...s, instancesGenerated: n }
}
const mwf = ['MONDAY', 'WEDNESDAY', 'FRIDAY'], all = DOW
addSchedule({ flightNumber: 'XY101', sourceAirport: 'DXB', destinationAirport: 'LHR', departureTime: '09:30', arrivalTime: '13:45', arrivalDayOffset: 0, aircraftId: 1, daysOfOperation: mwf })
addSchedule({ flightNumber: 'XY102', sourceAirport: 'LHR', destinationAirport: 'DXB', departureTime: '15:30', arrivalTime: '01:15', arrivalDayOffset: 1, aircraftId: 1, daysOfOperation: mwf })
addSchedule({ flightNumber: 'XY201', sourceAirport: 'DXB', destinationAirport: 'LHR', departureTime: '21:00', arrivalTime: '01:20', arrivalDayOffset: 1, aircraftId: 3, daysOfOperation: all })

const cap = (i) => aircraft[i.s.aircraftId].rows * aircraft[i.s.aircraftId].letters.length
const active = (i) => [...i.seats.entries()].filter(([, b]) => b.status === 'CONFIRMED').map(([k]) => k)
const view = (b) => ({ pnr: b.pnr, flightInstanceId: b.inst.id, flightNumber: b.inst.s.flightNumber, flightDate: b.inst.date, departureTime: b.inst.dep,
  passengerCount: b.passengers.length, seats: b.passengers.map((p) => p.seat), passengers: b.passengers, status: b.status, createdAt: b.createdAt, cancelledAt: b.cancelledAt || null })
const err = (res, status, message, details = []) => send(res, status, { timestamp: new Date().toISOString(), status, error: '', message, path: '', details })
const send = (res, status, body) => { res.writeHead(status, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(body)) }

http.createServer((req, res) => {
  const url = new URL(req.url, 'http://x'); const p = url.pathname; let body = ''
  req.on('data', (c) => (body += c)); req.on('end', () => {
    const json = body ? JSON.parse(body) : null; let m
    if (p === '/api/v1/airports') return send(res, 200, airports)
    if (p === '/api/v1/admin/schedules' && req.method === 'GET') return send(res, 200, schedules)
    if (p === '/api/v1/admin/schedules' && req.method === 'POST') {
      if (schedules.some((s) => s.flightNumber === json.flightNumber)) return err(res, 409, `Flight number already exists: ${json.flightNumber}`)
      if (json.sourceAirport === json.destinationAirport) return err(res, 400, 'Source and destination airports must differ')
      return send(res, 201, addSchedule(json))
    }
    if (p === '/api/v1/flights/search') {
      const { source, destination, date } = Object.fromEntries(url.searchParams)
      return send(res, 200, instances.filter((i) => i.s.sourceAirport === source && i.s.destinationAirport === destination && i.date === date).map((i) => ({
        flightInstanceId: i.id, flightNumber: i.s.flightNumber, sourceAirport: source, destinationAirport: destination, flightDate: i.date,
        departureTime: i.dep, arrivalTime: i.arr, availableSeats: cap(i) - active(i).length, totalSeats: cap(i) })))
    }
    if ((m = p.match(/^\/api\/v1\/flights\/(\d+)\/seats$/))) {
      const i = instances[m[1] - 1]; if (!i) return err(res, 404, 'Flight not found'); const a = aircraft[i.s.aircraftId]; const taken = new Set(active(i)); const seats = []
      for (let r = 1; r <= a.rows; r++) for (const l of a.letters) seats.push({ seat: `${r}${l}`, status: taken.has(`${r}${l}`) ? 'BOOKED' : 'AVAILABLE' })
      return send(res, 200, { flightInstanceId: i.id, flightNumber: i.s.flightNumber, flightDate: i.date, aircraftType: a.type, totalSeats: cap(i), availableSeats: cap(i) - taken.size, seats })
    }
    if (p === '/api/v1/bookings' && req.method === 'POST') {
      const i = instances[json.flightInstanceId - 1]; if (!i) return err(res, 404, 'Flight not found')
      const taken = json.passengers.map((x) => x.seat).filter((s) => active(i).includes(s))
      if (taken.length) return err(res, 409, `Seats already booked: [${taken.join(', ')}]`)
      const b = { pnr: randomUUID(), inst: i, status: 'CONFIRMED', passengers: json.passengers.map(({ name, seat }) => ({ name, seat })), createdAt: new Date().toISOString() }
      b.passengers.forEach((x) => i.seats.set(x.seat, b)); bookings.set(b.pnr, b); return send(res, 201, view(b))
    }
    if ((m = p.match(/^\/api\/v1\/bookings\/([^/]+)(\/cancel)?$/))) {
      const b = bookings.get(m[1]); if (!b) return err(res, 404, `Booking not found: ${m[1]}`)
      if (!m[2]) return send(res, 200, view(b))
      if (b.status === 'CANCELLED') return err(res, 409, 'Booking is already cancelled')
      b.status = 'CANCELLED'; b.cancelledAt = new Date().toISOString(); b.passengers.forEach((x) => b.inst.seats.delete(x.seat)); return send(res, 200, view(b))
    }
    err(res, 404, 'Not found')
  })
}).listen(8080, () => console.log('Mock API on http://localhost:8080'))
