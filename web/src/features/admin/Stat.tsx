/** A number card of the ADMIN area (prototype "Quản trị"): label, big value, optional detail line. */
export function Stat({ label, value, sub }: { label: string; value: string | number; sub?: string }) {
  return (
    <div className="pw-admin-stat">
      <span className="pw-admin-stat-label">{label}</span>
      <span className="pw-admin-stat-value">{value}</span>
      {sub ? <span className="pw-admin-stat-sub">{sub}</span> : null}
    </div>
  );
}
