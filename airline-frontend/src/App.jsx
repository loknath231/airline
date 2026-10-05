import { NavLink, Route, Routes } from 'react-router-dom'
import SearchPage from './pages/SearchPage.jsx'
import BookingPage from './pages/BookingPage.jsx'
import AdminPage from './pages/AdminPage.jsx'

export default function App() {
  return (
    <>
      <header className="topbar">
        <div className="wrap topbar-inner">
          <NavLink to="/" className="brand">
            <span className="brand-mark" aria-hidden="true">✈</span> SkyBook
          </NavLink>
          <nav className="nav" aria-label="Main">
            <NavLink to="/" end>Book a flight</NavLink>
            <NavLink to="/bookings">My bookings</NavLink>
            <NavLink to="/admin">Back office</NavLink>
          </nav>
          <div className="topbar-links">
            <a href="/swagger-ui.html" target="_blank" rel="noreferrer">API docs</a>
          </div>
        </div>
      </header>
      <main className="wrap">
        <Routes>
          <Route path="/" element={<SearchPage />} />
          <Route path="/bookings" element={<BookingPage />} />
          <Route path="/bookings/:pnr" element={<BookingPage />} />
          <Route path="/admin" element={<AdminPage />} />
          <Route path="*" element={<p className="muted">Page not found.</p>} />
        </Routes>
      </main>
      <footer className="wrap footer muted small">All times are shown in UTC. Bookings are open up to 365 days ahead.</footer>
    </>
  )
}
