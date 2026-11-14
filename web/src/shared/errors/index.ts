import { ApiError, type ErrorDetail } from '@/data/apiError';
import { errorMessages, jobErrorMessages } from './messages';

export { errorMessages, jobErrorMessages };

const NETWORK_MESSAGE = 'Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.';

/** "4 phút 12 giây", "45 giây", "2 giờ 5 phút" for a wait in seconds. */
export function formatWait(seconds: number): string {
  const s = Math.max(0, Math.ceil(seconds));
  const hours = Math.floor(s / 3600);
  const minutes = Math.floor((s % 3600) / 60);
  const rest = s % 60;
  if (hours > 0) return minutes > 0 ? `${hours} giờ ${minutes} phút` : `${hours} giờ`;
  if (minutes > 0) return rest > 0 ? `${minutes} phút ${rest} giây` : `${minutes} phút`;
  return `${rest} giây`;
}

/**
 * The sentence to show for an error (docs/client/ARCHITECTURE.md §3.2, UX_BRIEF §2, §6.1). A code without a sentence
 * shows the generic sentence with the lookup code, never the raw code alone.
 */
export function messageFor(error: unknown): string {
  if (!(error instanceof ApiError)) return NETWORK_MESSAGE;
  if (error.code === 'NETWORK_ERROR') return NETWORK_MESSAGE;
  const template = errorMessages[error.code];
  const requestId = error.requestId ?? error.code;
  if (!template) return `Đã có lỗi xảy ra. Mã tra cứu: ${requestId}.`;
  return template
    .replace('{Retry-After}', error.retryAfter !== null ? formatWait(error.retryAfter) : 'ít phút')
    .replace('{X-Request-Id}', requestId);
}

/** Sentence for a failed job's `errorCode` (UX_BRIEF §6.1). */
export function jobMessageFor(code: string | null | undefined): string {
  if (!code) return 'Đã có lỗi xảy ra.';
  return jobErrorMessages[code] ?? errorMessages[code] ?? `Đã có lỗi xảy ra (${code}).`;
}

const FIELD_SENTENCES: Record<string, string> = {
  INVALID_EMAIL_FORMAT: 'Email không đúng định dạng.',
  PHONE_NUMBER_INVALID: 'Số điện thoại không hợp lệ.',
  PHONE_NUMBER_WRONG_REGION: 'Chỉ nhận số Việt Nam.',
  PHONE_NUMBER_TOO_SHORT: 'Số điện thoại quá ngắn.',
  PHONE_NUMBER_TOO_LONG: 'Số điện thoại quá dài.',
  PHONE_NUMBER_TYPE_NOT_ALLOWED: 'Số điện thoại không hợp lệ.',
  INVALID_TIME_WINDOW: 'Giờ kết thúc phải sau giờ bắt đầu.',
  INVALID_SHIFT: 'Giờ kết thúc ca phải sau giờ bắt đầu.',
  COORDINATES_INCOMPLETE: 'Cần đủ cả vĩ độ và kinh độ.',
  ADDRESS_OR_COORDINATES_REQUIRED: 'Nhập địa chỉ hoặc đặt ghim trên bản đồ.',
  INVALID_CAPACITY: 'Sức chứa phải lớn hơn 0.',
  // Choices (select): "chọn" rather than "nhập" (prototype "Xe").
  CAPACITY_UNIT_REQUIRED: 'Vui lòng chọn đơn vị.',
  ROLE_REQUIRED: 'Vui lòng chọn vai trò.',
  DRIVER_REQUIRED: 'Vui lòng chọn tài xế.',
  VEHICLE_REQUIRED: 'Vui lòng chọn xe.',
  INVALID_DEMAND: 'Số lượng phải là số nguyên, không âm.',
  INVALID_MAX_DISTANCE: 'Quãng đường tối đa phải lớn hơn 0.',
  INVALID_SERVICE_TIME: 'Thời gian phục vụ từ 0 đến 24 giờ.',
  // Stop import (T61, UX_BRIEF §6.2): errors on the file, and on a time of a row.
  INVALID_TIME_FORMAT: 'Giờ không đúng dạng, ví dụ 09:00.',
  UNSUPPORTED_FILE_TYPE: 'Chỉ nhận file .xlsx hoặc .csv.',
  UNREADABLE_FILE: 'File bị hỏng hoặc không đúng định dạng.',
  NO_ROWS: 'File không có dòng điểm giao nào.',
  LABEL_COLUMN_REQUIRED: 'File thiếu cột "Nhãn / mã đơn". Tải file mẫu để xem các cột.',
  // Plan copy (T62).
  COPY_SCOPE_REQUIRED: 'Vui lòng chọn điểm giao cần chép.',
  // Date ranges (T60, T63); DATE_RANGE_TOO_LONG depends on the screen's limit.
  INVALID_DATE_RANGE: 'Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.',
};

/**
 * Sentence for one `details[].code` (UX_BRIEF §6.2). `label` is the field's name on screen ("Tên xe").
 */
export function fieldMessage(code: string, label: string): string {
  const sentence = FIELD_SENTENCES[code];
  if (sentence) return sentence;
  if (code.endsWith('_REQUIRED')) return `Vui lòng nhập ${label.toLocaleLowerCase('vi-VN')}.`;
  if (/^INVALID_.*_LENGTH$/.test(code)) return `${label} quá dài hoặc quá ngắn.`;
  return 'Giá trị không hợp lệ.';
}

/**
 * Field errors of a VALIDATION_ERROR keyed by the request path (`stops[3].timeWindow.end`), ready for a form library.
 * Details without a field are returned under the empty key.
 */
export function fieldErrors(error: unknown, labels: Record<string, string> = {}): Record<string, string> {
  if (!(error instanceof ApiError)) return {};
  const result: Record<string, string> = {};
  error.details.forEach((detail: ErrorDetail) => {
    const field = detail.field ?? '';
    if (result[field]) return;
    const leaf =
      field
        .replace(/\[\d+\]/g, '')
        .split('.')
        .pop() ?? field;
    result[field] = fieldMessage(detail.code, labels[field] ?? labels[leaf] ?? 'giá trị này');
  });
  return result;
}
