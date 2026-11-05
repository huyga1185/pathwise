/**
 * The depot icon (DESIGN_SYSTEM "Màu route trên bản đồ"): a warehouse filled with the brand colour #0b6a5c in both
 * themes, white outline and door, light shadow, no square background.
 */
const HOUSE = 'M2.5 10.5L12 3.5l9.5 7V21h-19z';
const DOOR = 'M9.2 21v-6.2h5.6V21z';

export const DEPOT_SVG = `<svg viewBox="0 0 24 24" width="30" height="30" aria-hidden="true" class="pw-depot-svg"><path d="${HOUSE}" fill="#0b6a5c" stroke="#ffffff" stroke-width="1.6" stroke-linejoin="round"/><path d="${DOOR}" fill="#ffffff"/></svg>`;

/** The same icon for legends next to a map. */
export function DepotIcon({ size = 20 }: { size?: number }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true" focusable="false" className="pw-depot-svg">
      <path d={HOUSE} fill="#0b6a5c" stroke="#ffffff" strokeWidth="1.6" strokeLinejoin="round" />
      <path d={DOOR} fill="#ffffff" />
    </svg>
  );
}
