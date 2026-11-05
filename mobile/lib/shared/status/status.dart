/// API status → label and tone, the status table of docs/client/DESIGN_SYSTEM.md (same as the web's statusInfo).
/// Screens never write status labels themselves.
enum Tone { muted, info, signal, danger, success, route }

enum StatusKind { plan, route, stop, dropReason, geocode, processing, invitation, member, vehicle, job }

const _status = <StatusKind, Map<String, (String, Tone)>>{
  StatusKind.plan: {
    'DRAFT': ('NHÁP', Tone.muted),
    'PUBLISHED': ('ĐÃ GIAO VIỆC', Tone.info),
    'IN_PROGRESS': ('ĐANG GIAO', Tone.signal),
    'COMPLETED': ('HOÀN THÀNH', Tone.success),
    'CANCELLED': ('ĐÃ HUỶ', Tone.danger),
  },
  StatusKind.route: {
    'PENDING': ('CHỜ GIAO', Tone.muted),
    'IN_PROGRESS': ('ĐANG GIAO', Tone.signal),
    'COMPLETED': ('HOÀN THÀNH', Tone.success),
  },
  StatusKind.stop: {
    'PENDING': ('CHỜ GIAO', Tone.muted),
    'DELIVERED': ('ĐÃ GIAO', Tone.success),
    'FAILED': ('KHÔNG GIAO ĐƯỢC', Tone.danger),
    'SKIPPED': ('BỎ QUA', Tone.signal),
  },
  StatusKind.dropReason: {
    'NOT_OPTIMIZED': ('CHƯA XẾP TUYẾN', Tone.muted),
    'INFEASIBLE': ('KHÔNG XẾP ĐƯỢC', Tone.signal),
  },
  StatusKind.geocode: {'PENDING': ('ĐANG TÌM TOẠ ĐỘ', Tone.info), 'FAILED': ('KHÔNG TÌM THẤY', Tone.danger)},
  StatusKind.processing: {'GEOCODING': ('ĐANG TÌM TOẠ ĐỘ', Tone.info), 'OPTIMIZING': ('ĐANG TỐI ƯU', Tone.info)},
  StatusKind.invitation: {
    'PENDING': ('CHỜ NHẬN', Tone.info),
    'ACCEPTED': ('ĐÃ NHẬN', Tone.success),
    'REVOKED': ('ĐÃ THU HỒI', Tone.muted),
    'EXPIRED': ('HẾT HẠN', Tone.muted),
  },
  StatusKind.member: {
    'PENDING_VERIFICATION': ('CHƯA XÁC THỰC', Tone.muted),
    'ACTIVE': ('ĐANG HOẠT ĐỘNG', Tone.success),
    'DISABLED': ('ĐÃ VÔ HIỆU HOÁ', Tone.danger),
  },
  StatusKind.vehicle: {'ACTIVE': ('ĐANG DÙNG', Tone.success), 'INACTIVE': ('ĐÃ TẮT', Tone.muted)},
  StatusKind.job: {
    'QUEUED': ('ĐANG CHỜ', Tone.muted),
    'RUNNING': ('ĐANG CHẠY', Tone.info),
    'SUCCEEDED': ('XONG', Tone.success),
    'FAILED': ('LỖI', Tone.danger),
  },
};

/// The pill of a status, or null when that status has none (geocode RESOLVED, processing IDLE).
({String label, Tone tone})? statusInfo(StatusKind kind, String? status) {
  final hit = status == null ? null : _status[kind]?[status];
  return hit == null ? null : (label: hit.$1, tone: hit.$2);
}
