/**
 * API status → label and tone, the status table of docs/client/DESIGN_SYSTEM.md (from the design system's
 * reference components). Screens never write status labels themselves.
 */
export type Tone = 'muted' | 'info' | 'signal' | 'danger' | 'success' | 'route';
export type StatusKind =
  'plan' | 'route' | 'stop' | 'dropReason' | 'geocode' | 'processing' | 'invitation' | 'member' | 'vehicle' | 'job';

const STATUS: Record<StatusKind, Record<string, [string, Tone]>> = {
  plan: {
    DRAFT: ['NHÁP', 'muted'],
    PUBLISHED: ['ĐÃ GIAO VIỆC', 'info'],
    IN_PROGRESS: ['ĐANG GIAO', 'signal'],
    COMPLETED: ['HOÀN THÀNH', 'success'],
    CANCELLED: ['ĐÃ HUỶ', 'danger'],
  },
  route: { PENDING: ['CHỜ GIAO', 'muted'], IN_PROGRESS: ['ĐANG GIAO', 'signal'], COMPLETED: ['HOÀN THÀNH', 'success'] },
  stop: {
    PENDING: ['CHỜ GIAO', 'muted'],
    DELIVERED: ['ĐÃ GIAO', 'success'],
    FAILED: ['KHÔNG GIAO ĐƯỢC', 'danger'],
    SKIPPED: ['BỎ QUA', 'signal'],
  },
  dropReason: {
    NOT_OPTIMIZED: ['CHƯA XẾP TUYẾN', 'muted'],
    INFEASIBLE: ['KHÔNG XẾP ĐƯỢC', 'signal'],
    MANUAL: ['ĐÃ BỎ RA', 'muted'],
  },
  geocode: { PENDING: ['ĐANG TÌM TOẠ ĐỘ', 'info'], FAILED: ['KHÔNG TÌM THẤY', 'danger'] },
  processing: { GEOCODING: ['ĐANG TÌM TOẠ ĐỘ', 'info'], OPTIMIZING: ['ĐANG TỐI ƯU', 'info'] },
  invitation: {
    PENDING: ['CHỜ NHẬN', 'info'],
    ACCEPTED: ['ĐÃ NHẬN', 'success'],
    REVOKED: ['ĐÃ THU HỒI', 'muted'],
    EXPIRED: ['HẾT HẠN', 'muted'],
  },
  member: {
    PENDING_VERIFICATION: ['CHƯA XÁC THỰC', 'muted'],
    ACTIVE: ['ĐANG HOẠT ĐỘNG', 'success'],
    DISABLED: ['ĐÃ VÔ HIỆU HOÁ', 'danger'],
  },
  vehicle: { ACTIVE: ['ĐANG DÙNG', 'success'], INACTIVE: ['ĐÃ TẮT', 'muted'] },
  job: {
    QUEUED: ['ĐANG CHỜ', 'muted'],
    RUNNING: ['ĐANG CHẠY', 'info'],
    SUCCEEDED: ['XONG', 'success'],
    FAILED: ['LỖI', 'danger'],
  },
};

/** @returns the pill of a status, or null when that status has none (geocode RESOLVED, processing IDLE) */
export function statusInfo(kind: StatusKind, status: string | null | undefined): { label: string; tone: Tone } | null {
  const hit = status ? STATUS[kind][status] : undefined;
  return hit ? { label: hit[0], tone: hit[1] } : null;
}
