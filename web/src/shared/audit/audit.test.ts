import { describe, expect, it } from 'vitest';
import { AUDIT_ACTION_LABELS, AUDIT_ACTIONS, auditSentence } from './index';

describe('auditSentence', () => {
  it('mỗi hành động có câu theo UX_BRIEF §5.4', () => {
    expect(
      auditSentence({
        action: 'MEMBER_UPDATED',
        details: {
          email: 'a@b.vn',
          changes: { role: { from: 'DRIVER', to: 'DISPATCHER' }, canDrive: { from: false, to: true } },
        },
      }),
    ).toBe('Đổi thành viên a@b.vn: vai trò Tài xế → Điều phối, bật giao hàng');
    expect(auditSentence({ action: 'PLAN_ROUTES_EDITED', targetId: 811, details: { violations: 1 } })).toBe(
      'Chỉnh tay tuyến kế hoạch #811 (còn 1 vi phạm)',
    );
    expect(auditSentence({ action: 'STORE_LOCKED', details: { reason: 'Nợ phí' } })).toBe('Tạm khoá cửa hàng: Nợ phí');
    expect(auditSentence({ action: 'STORE_OWNER_TRANSFERRED', details: { from: 'a@b.vn', to: 'c@d.vn' } })).toBe(
      'Chuyển quyền chủ cửa hàng từ a@b.vn sang c@d.vn',
    );
  });

  it('chi tiết bị cắt và hành động lạ vẫn hiện được', () => {
    expect(auditSentence({ action: 'PLAN_PUBLISHED', details: { truncated: '{"x' } })).toBe(
      'Giao việc kế hoạch (chi tiết quá dài)',
    );
    expect(auditSentence({ action: 'SOMETHING_NEW', details: {} })).toBe('SOMETHING_NEW');
    for (const action of AUDIT_ACTIONS) expect(AUDIT_ACTION_LABELS[action]).toBeTruthy();
  });
});
