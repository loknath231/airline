// Thin client for the Spring Boot API. All paths are relative; Vite proxies /api in dev.
export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || `Request failed (${status})`)
    this.status = status
    this.details = body?.details || []
  }
}

async function request(path, options = {}) {
  let res
  try {
    res = await fetch(path, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    })
  } catch {
    throw new ApiError(0, { message: 'Cannot reach the server. Is the backend running on port 8080?' })
  }
  let body = null
  const text = await res.text()
  if (text) {
    try { body = JSON.parse(text) } catch { body = { message: text } }
  }
  if (!res.ok) throw new ApiError(res.status, body)
  return body
}

const qs = (o) => new URLSearchParams(o).toString()

export const api = {
  airports: () => request('/api/v1/airports'),
  searchFlights: ({ source, destination, date }) =>
    request(`/api/v1/flights/search?${qs({ source, destination, date })}`),
  seatMap: (flightInstanceId) => request(`/api/v1/flights/${flightInstanceId}/seats`),
  createBooking: (payload) =>
    request('/api/v1/bookings', { method: 'POST', body: JSON.stringify(payload) }),
  getBooking: (pnr) => request(`/api/v1/bookings/${encodeURIComponent(pnr)}`),
  cancelBooking: (pnr) =>
    request(`/api/v1/bookings/${encodeURIComponent(pnr)}/cancel`, { method: 'POST' }),
  listSchedules: () => request('/api/v1/admin/schedules'),
  createSchedule: (payload) =>
    request('/api/v1/admin/schedules', { method: 'POST', body: JSON.stringify(payload) }),
}

// Aircraft have no list endpoint (pre-configured, CRUD out of scope) - mirror the seeded data.
export const AIRCRAFT = [
  { id: 1, label: 'A320 - 30 rows x ABCDEF (180 seats)' },
  { id: 2, label: 'B737 - 25 rows x ABCDEF (150 seats)' },
  { id: 3, label: 'A330 - 40 rows x ABCDEFGH (320 seats)' },
]

// Remember PNRs on this device (the API has no "list my bookings" endpoint).
const KEY = 'skybook.pnrs'
export const savedPnrs = () => {
  try { return JSON.parse(localStorage.getItem(KEY)) || [] } catch { return [] }
}
export const rememberPnr = (pnr) => {
  try {
    localStorage.setItem(KEY, JSON.stringify([pnr, ...savedPnrs().filter((p) => p !== pnr)].slice(0, 20)))
  } catch { /* storage unavailable */ }
}
