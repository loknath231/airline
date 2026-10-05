export const fmtTime = (iso) =>
  new Date(iso).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit', timeZone: 'UTC' })
export const fmtDate = (d) =>
  new Date(d + 'T00:00:00Z').toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' })
export const fmtDateTime = (iso) => `${fmtDate(iso.slice(0, 10))} ${fmtTime(iso)} UTC`
export const durationLabel = (dep, arr) => {
  const m = Math.round((new Date(arr) - new Date(dep)) / 60000)
  return `${Math.floor(m / 60)}h ${String(m % 60).padStart(2, '0')}m`
}
export const todayUtc = (plusDays = 0) => {
  const d = new Date()
  d.setUTCDate(d.getUTCDate() + plusDays)
  return d.toISOString().slice(0, 10)
}
