/** A number card (prototypes "Quản trị — tổng quan", "Báo cáo"): label, big value, optional detail line. */
export function Stat({ label, value, sub }: { label: string; value: string | number; sub?: string }) {
  return (
    <div className="pw-numcard">
      <span className="pw-numcard-label">{label}</span>
      <span className="pw-numcard-value">{value}</span>
      {sub ? <span className="pw-numcard-sub">{sub}</span> : null}
    </div>
  );
}
