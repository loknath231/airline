# SkyBook - React frontend for the Airline Ticketing API

React 19 + Vite 8 + React Router 7. No UI framework: plain CSS with automatic light/dark mode.

## Features (one screen per API area)

| Page | Route | API used |
|---|---|---|
| Search flights | `/` | `GET /api/v1/airports`, `GET /api/v1/flights/search` |
| Seat map + booking + confirmation | `/` | `GET /api/v1/flights/{id}/seats`, `POST /api/v1/bookings` |
| My bookings: look up by PNR, cancel | `/bookings`, `/bookings/:pnr` | `GET /api/v1/bookings/{pnr}`, `POST /api/v1/bookings/{pnr}/cancel` |
| Back office: create / list schedules | `/admin` | `POST` and `GET /api/v1/admin/schedules` |

Details: live seat map with aisle, up to 9 seats per booking, one name field per seat, 409 "seat taken" handling
(map refreshes and stale seats are deselected), cancel confirmation, PNRs remembered on this device,
server validation messages shown inline, all times in UTC.

## Run

Prerequisites: Node 20+ and the Spring Boot backend running on port 8080.

```bash
npm install
npm run dev            # http://localhost:5173  (proxies /api to http://localhost:8080)
```

Other backend address: `BACKEND_URL=http://host:8080 npm run dev`.

No backend handy? `npm run mock` starts an in-memory fake of the API on :8080 with the same demo data.

Production build: `npm run build` creates `dist/`. Serve it from the same origin as the API (for example copy `dist/*` into the
Spring Boot `src/main/resources/static/`), or add CORS to the backend and change `src/api.js` to use an absolute base URL.

## Structure

```
src/api.js            fetch client, error type, saved PNRs, seeded aircraft list
src/format.js         UTC date/time helpers
src/components/       SeatMap, BookingCard, ErrorBox
src/pages/            SearchPage, BookingPage, AdminPage
src/styles.css        design tokens + layout
mock-server.js        optional in-memory API for UI work
```
