/**
 * Audit events as sentences (UX_BRIEF §5.4, T58): one place for the ADMIN log and the OWNER's store log. An action
 * without a sentence shows its code; the list of actions only grows (API_SPEC §3a).
 */

/** The fields of an audit event the sentence needs (both `AuditEventResponse` and `StoreAuditEventResponse`). */
export interface AuditLine {
  action: string;
  targetId?: number | null;
  actorEmail?: string | null;
  actorRole?: string | null;
  details: Record<string, unknown>;
}

export const AUDIT_ACTIONS = [
  'MEMBER_UPDATED',
  'INVITATION_REVOKED',
  'PLAN_PUBLISHED',
  'PLAN_CANCELLED',
  'PLAN_ROUTES_EDITED',
  'STORE_VIEWED',
  'STORE_LOCKED',
  'STORE_UNLOCKED',
  'USER_DISABLED',
  'USER_ENABLED',
  'USER_SESSIONS_REVOKED',
  'VERIFICATION_EMAIL_SENT',
  'PASSWORD_RESET_LINK_SENT',
  'STORE_OWNER_TRANSFERRED',
] as const;

/** Label of each action in the filter. */
export const AUDIT_ACTION_LABELS: Record<string, string> = {
  MEMBER_UPDATED: 'Đổi thành viên',
  INVITATION_REVOKED: 'Huỷ lời mời',
  PLAN_PUBLISHED: 'Giao việc kế hoạch',
  PLAN_CANCELLED: 'Huỷ kế hoạch',
  PLAN_ROUTES_EDITED: 'Chỉnh tay tuyến',
  STORE_VIEWED: 'Xem thông tin cửa hàng',
  STORE_LOCKED: 'Khoá cửa hàng',
  STORE_UNLOCKED: 'Mở khoá cửa hàng',
  USER_DISABLED: 'Khoá tài khoản',
  USER_ENABLED: 'Mở khoá tài khoản',
  USER_SESSIONS_REVOKED: 'Buộc đăng xuất',
  VERIFICATION_EMAIL_SENT: 'Gửi lại email xác thực',
  PASSWORD_RESET_LINK_SENT: 'Gửi link đặt lại mật khẩu',
  STORE_OWNER_TRANSFERRED: 'Chuyển chủ cửa hàng',
};

const ROLE: Record<string, string> = { OWNER: 'Chủ cửa hàng', DISPATCHER: 'Điều phối', DRIVER: 'Tài xế' };
const STATUS: Record<string, string> = { ACTIVE: 'mở khoá', DISABLED: 'khoá', PENDING_VERIFICATION: 'chờ xác thực' };

function text(value: unknown): string {
  return typeof value === 'string' ? value : value == null ? '' : String(value);
}

/** "vai trò Điều phối → Tài xế, khoá, bật giao hàng" for `details.changes` of MEMBER_UPDATED. */
function memberChanges(changes: unknown): string {
  if (!changes || typeof changes !== 'object') return '';
  const parts: string[] = [];
  const entries = changes as Record<string, { from?: unknown; to?: unknown }>;
  if (entries.role) parts.push(`vai trò ${ROLE[text(entries.role.from)] ?? ''} → ${ROLE[text(entries.role.to)] ?? ''}`);
  if (entries.status) parts.push(STATUS[text(entries.status.to)] ?? text(entries.status.to));
  if (entries.canDrive) parts.push(entries.canDrive.to ? 'bật giao hàng' : 'tắt giao hàng');
  return parts.join(', ');
}

/** The sentence of one event; the actor (email or "Quản trị viên") is shown next to it, not inside. */
export function auditSentence(line: AuditLine): string {
  const d = line.details;
  if ('truncated' in d) return `${AUDIT_ACTION_LABELS[line.action] ?? line.action} (chi tiết quá dài)`;
  const plan = `kế hoạch #${line.targetId ?? ''}`;
  switch (line.action) {
    case 'MEMBER_UPDATED':
      return `Đổi thành viên ${text(d.email)}: ${memberChanges(d.changes)}`;
    case 'INVITATION_REVOKED':
      return `Huỷ lời mời ${text(d.email)} (${ROLE[text(d.role)] ?? text(d.role)})`;
    case 'PLAN_PUBLISHED':
      return `Giao việc ${plan}`;
    case 'PLAN_CANCELLED':
      return `Huỷ ${plan}`;
    case 'PLAN_ROUTES_EDITED': {
      const violations = Number(d.violations ?? 0);
      return violations > 0 ? `Chỉnh tay tuyến ${plan} (còn ${violations} vi phạm)` : `Chỉnh tay tuyến ${plan}`;
    }
    case 'STORE_VIEWED':
      return 'Xem thông tin cửa hàng';
    case 'STORE_LOCKED':
      return `Tạm khoá cửa hàng: ${text(d.reason)}`;
    case 'STORE_UNLOCKED':
      return 'Mở khoá cửa hàng';
    case 'USER_DISABLED':
      return `Khoá tài khoản ${text(d.email)}`;
    case 'USER_ENABLED':
      return `Mở khoá tài khoản ${text(d.email)}`;
    case 'USER_SESSIONS_REVOKED':
      return `Đăng xuất ${text(d.email)} khỏi mọi thiết bị`;
    case 'VERIFICATION_EMAIL_SENT':
      return `Gửi lại email xác thực cho ${text(d.email)}`;
    case 'PASSWORD_RESET_LINK_SENT':
      return `Gửi link đặt lại mật khẩu cho ${text(d.email)}`;
    case 'STORE_OWNER_TRANSFERRED':
      return d.from
        ? `Chuyển quyền chủ cửa hàng từ ${text(d.from)} sang ${text(d.to)}`
        : `Chuyển quyền chủ cửa hàng cho ${text(d.to)}`;
    default:
      return line.action;
  }
}
