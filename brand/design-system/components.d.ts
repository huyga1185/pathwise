import type * as React from 'react';

export type Tone = 'muted' | 'info' | 'signal' | 'danger' | 'success' | 'route';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  /** primary: one per screen. danger: destructive. success / signal: driver result buttons only. */
  variant?: 'primary' | 'secondary' | 'quiet' | 'danger' | 'success' | 'signal';
  /** md 36px (web) · lg 48px (driver app minimum) · xl 56px (driver result buttons). */
  size?: 'md' | 'lg' | 'xl';
  /** Disables the button and sets aria-busy while a request runs. */
  busy?: boolean;
  /** Full width. */
  block?: boolean;
}
export declare function Button(props: ButtonProps): React.ReactElement;

export interface TextFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: React.ReactNode;
  /** Help text under the field. */
  hint?: React.ReactNode;
  /** Error sentence from UX_BRIEF §6; turns the border danger. */
  error?: React.ReactNode;
  /** Renders a textarea. */
  multiline?: boolean;
}
export declare function TextField(props: TextFieldProps): React.ReactElement;

export type StatusKind = 'plan' | 'route' | 'stop' | 'dropReason' | 'geocode' | 'processing' | 'invitation' | 'member' | 'job';
export interface StatusPillProps {
  /** Which API enum `status` belongs to. With kind + status the label and tone come from the README table. */
  kind?: StatusKind;
  /** The API value, e.g. "PUBLISHED". */
  status?: string;
  /** Override the tone. */
  tone?: Tone;
  /** Override the label. */
  children?: React.ReactNode;
  className?: string;
}
/** Renders nothing for a status with no pill (e.g. geocode RESOLVED). */
export declare function StatusPill(props: StatusPillProps): React.ReactElement | null;

export interface ProgressBarProps {
  label: string;
  /** Leave value/max out for an indeterminate bar. */
  value?: number;
  max?: number;
  className?: string;
}
export declare function ProgressBar(props: ProgressBarProps): React.ReactElement;

export interface DataTableColumn<Row> {
  key: string;
  header: React.ReactNode;
  render?: (row: Row) => React.ReactNode;
  /** Right-aligned tabular numbers (ETA, km, load). */
  numeric?: boolean;
  align?: 'start' | 'end';
  width?: string | number;
}
export interface DataTableProps<Row> {
  columns: DataTableColumn<Row>[];
  rows: Row[];
  /** Property name or function; default "id". */
  rowKey?: string | ((row: Row) => React.Key);
  selectedKey?: React.Key | null;
  onRowClick?: (row: Row) => void;
  /** Shown when rows is empty. */
  empty?: React.ReactNode;
  /** Accessible name (visually hidden caption). */
  caption?: string;
  className?: string;
}
export declare function DataTable<Row>(props: DataTableProps<Row>): React.ReactElement;

export interface StopCardProps {
  sequence?: number;
  /** "ĐH-1009 · Chị Lan" */
  title: React.ReactNode;
  address: React.ReactNode;
  /** Formatted local time, "10:39". */
  eta?: string;
  /** "09:00–11:00" */
  timeWindow?: string;
  note?: React.ReactNode;
  /** Stop status from the API. */
  status?: 'PENDING' | 'DELIVERED' | 'FAILED' | 'SKIPPED';
  failureReason?: string;
  /** E.164 from the API; shown as 0905 123 456 with a call button. */
  phone?: string;
  /** The driver's next stop. */
  current?: boolean;
  /** Result buttons etc. */
  actions?: React.ReactNode;
  className?: string;
}
export declare function StopCard(props: StopCardProps): React.ReactElement;

export interface MapMarkerProps {
  sequence?: number;
  tone?: 'route' | 'signal' | 'success' | 'danger' | 'muted';
  /** 0-based route position in the plan; colours the marker map-route-(n % 7 + 1) and overrides tone. */
  routeIndex?: number;
  selected?: boolean;
  /** Accessible name; default "Điểm <sequence>". */
  label?: string;
  className?: string;
}
/** Always rendered with light-theme colours (map tiles are light). */
export declare function MapMarker(props: MapMarkerProps): React.ReactElement;

export interface DialogProps {
  open: boolean;
  title: React.ReactNode;
  children?: React.ReactNode;
  confirmLabel: React.ReactNode;
  cancelLabel?: React.ReactNode;
  tone?: 'primary' | 'danger';
  /** md 440px (confirmations) · lg 720px (forms) · full: near full screen (map picker). */
  size?: 'md' | 'lg' | 'full';
  busy?: boolean;
  /** Disable the confirm button until the content is valid (e.g. no pin placed yet). */
  confirmDisabled?: boolean;
  onConfirm?: () => void;
  onClose?: () => void;
}
export declare function Dialog(props: DialogProps): React.ReactElement | null;

export interface ToastProps {
  tone?: 'info' | 'success' | 'danger';
  children: React.ReactNode;
  actionLabel?: string;
  onAction?: () => void;
  onClose?: () => void;
  className?: string;
}
export declare function Toast(props: ToastProps): React.ReactElement;

/** "+84905123456" → "0905 123 456". */
export declare function formatPhone(e164: string): string;
export declare function statusInfo(kind: StatusKind, status: string): { label: string; tone: Tone } | null;

declare global {
  interface Window {
    PathWise: {
      Button: typeof Button; TextField: typeof TextField; StatusPill: typeof StatusPill; ProgressBar: typeof ProgressBar;
      DataTable: typeof DataTable; StopCard: typeof StopCard; MapMarker: typeof MapMarker; Dialog: typeof Dialog; Toast: typeof Toast;
      formatPhone: typeof formatPhone; statusInfo: typeof statusInfo;
    };
  }
}
