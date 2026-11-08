import type { SVGProps } from 'react';

/** Outline icons, 1.5–2px stroke (DESIGN_SYSTEM "Biểu tượng"). Decorative unless the caller labels the control. */
function Icon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 24 24"
      width="20"
      height="20"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.7}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      {...props}
    />
  );
}

export const MailIcon = () => (
  <Icon>
    <rect x="3" y="5" width="18" height="14" rx="2" />
    <path d="M3.5 6.5l8.5 6.5 8.5-6.5" />
  </Icon>
);
export const LockIcon = () => (
  <Icon>
    <rect x="4.5" y="10.5" width="15" height="10" rx="2" />
    <path d="M8 10.5V7.5a4 4 0 0 1 8 0v3" />
  </Icon>
);
export const EyeIcon = () => (
  <Icon>
    <path d="M3 12s4-6 9-6 9 6 9 6-4 6-9 6-9-6-9-6z" />
    <circle cx="12" cy="12" r="3" />
  </Icon>
);
export const EyeOffIcon = () => (
  <Icon>
    <path d="M3 3l18 18" />
    <path d="M10.6 6.1A9.8 9.8 0 0 1 12 6c5 0 9 6 9 6a16 16 0 0 1-2.6 3.2M6.6 7.6C4.4 9.1 3 12 3 12s4 6 9 6a8.6 8.6 0 0 0 3.6-.8" />
    <path d="M9.9 10a3 3 0 0 0 4.1 4.1" />
  </Icon>
);
export const ArrowRightIcon = () => (
  <Icon strokeWidth={2}>
    <path d="M9 6l6 6-6 6" />
  </Icon>
);
export const SunIcon = () => (
  <Icon>
    <circle cx="12" cy="12" r="4" />
    <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
  </Icon>
);
export const MoonIcon = () => (
  <Icon>
    <path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z" />
  </Icon>
);

export const RouteIcon = () => (
  <Icon>
    <circle cx="5" cy="18.5" r="2" />
    <path d="M7 18.5h6a3.2 3.2 0 0 0 0-6.4H9.5a3.2 3.2 0 0 1 0-6.4H17" />
    <circle cx="19" cy="5.7" r="2" />
  </Icon>
);
export const PhoneIcon = () => (
  <Icon>
    <rect x="6.5" y="2.5" width="11" height="19" rx="2.5" />
    <path d="M10.5 18.5h3" />
  </Icon>
);
export const CheckCircleIcon = () => (
  <Icon>
    <circle cx="12" cy="12" r="9" />
    <path d="M8 12.3l2.7 2.7L16 9.6" />
  </Icon>
);

/** The PathWise mark (brand/logo/pathwise-mark.svg). `white` for brand and dark backgrounds (DESIGN_SYSTEM "Logo"). */
export function LogoMark({
  white = false,
  size = 34,
  dashes = true,
}: {
  white?: boolean;
  size?: number;
  dashes?: boolean;
}) {
  const gradientId = white ? undefined : 'pw-mark-gradient';
  return (
    <svg viewBox="0 0 108 102" width={size} height={(size * 102) / 108} aria-hidden="true" focusable="false">
      {gradientId ? (
        <defs>
          <linearGradient id={gradientId} gradientUnits="userSpaceOnUse" x1="40" y1="0" x2="62" y2="102">
            <stop offset="0" stopColor="#1fa189" />
            <stop offset="0.6" stopColor="#0b6a5c" />
            <stop offset="1" stopColor="#054a40" />
          </linearGradient>
        </defs>
      ) : null}
      <path
        d="M15.6 66V22A22 22 0 0 1 37.6 0H72A36 36 0 0 1 72 72C62 72 53 76 46.6 81.5C41 86.5 38 93 36.5 102H0.5C2 86 8 75 15.6 70L50 57.5L46 52.5Z"
        fill={white ? '#ffffff' : `url(#${gradientId})`}
      />
      {dashes ? (
        <path
          d="M49 64.5C40 67 32 73 27 81C24 87 22 94 21 102"
          fill="none"
          stroke={white ? '#0b6a5c' : '#ffffff'}
          strokeWidth="3"
          strokeDasharray="10 7"
          strokeLinecap="round"
        />
      ) : null}
      <path d="M61 69C61 69 41 51 41 35A20 20 0 0 1 81 35C81 51 61 69 61 69Z" fill={white ? '#0b6a5c' : '#ffffff'} />
      <path
        d="M61 53C61 53 50.5 45.5 50.5 37.5A10.5 10.5 0 0 1 71.5 37.5C71.5 45.5 61 53 61 53Z"
        fill={white ? '#ffffff' : '#14876f'}
      />
    </svg>
  );
}
export const StoreIcon = () => (
  <Icon>
    <path d="M4 10h16l-1.5-5h-13z" />
    <path d="M5 10v9h14v-9" />
    <path d="M10 19v-5h4v5" />
  </Icon>
);
export const PinIcon = () => (
  <Icon>
    <path d="M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z" />
    <circle cx="12" cy="9.5" r="2.5" />
  </Icon>
);
export const UserIcon = () => (
  <Icon>
    <circle cx="12" cy="8" r="4" />
    <path d="M4 21c0-4 3.6-7 8-7s8 3 8 7" />
  </Icon>
);
export const UserPlusIcon = () => (
  <Icon>
    <circle cx="9" cy="8" r="3.5" />
    <path d="M3 20c0-3.5 2.7-6 6-6s6 2.5 6 6" />
    <path d="M17 8v6M14 11h6" />
  </Icon>
);
export const TrashIcon = () => (
  <Icon>
    <path d="M5 7h14M10 7V4h4v3M7 7l1 13h8l1-13" />
  </Icon>
);
