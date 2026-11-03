import { describe, expect, it } from 'vitest';
import { ApiError } from '@/data/apiError';
import { fieldErrors, formatWait, messageFor } from './index';

describe('messageFor', () => {
  it('dùng câu của UX_BRIEF §6.1', () => {
    expect(messageFor(new ApiError({ status: 401, code: 'INVALID_EMAIL_OR_PASSWORD' }))).toBe(
      'Email hoặc mật khẩu không đúng.',
    );
  });

  it('điền thời gian chờ của RATE_LIMITED từ Retry-After', () => {
    expect(messageFor(new ApiError({ status: 429, code: 'RATE_LIMITED', retryAfter: 252 }))).toBe(
      'Bạn thao tác quá nhiều lần. Thử lại sau 4 phút 12 giây.',
    );
  });

  it('mã không có câu thì hiện câu chung kèm mã tra cứu', () => {
    expect(messageFor(new ApiError({ status: 500, code: 'SOMETHING_NEW', requestId: 'abc' }))).toBe(
      'Đã có lỗi xảy ra. Mã tra cứu: abc.',
    );
    expect(messageFor(new ApiError({ status: 500, code: 'INTERNAL_ERROR', requestId: 'abc' }))).toBe(
      'Đã có lỗi xảy ra. Mã tra cứu: abc.',
    );
  });

  it('lỗi mạng có câu riêng', () => {
    expect(messageFor(ApiError.network())).toBe('Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.');
  });
});

describe('fieldErrors', () => {
  it('gắn lỗi vào đúng đường dẫn field, kể cả phần tử của danh sách', () => {
    const error = new ApiError({
      status: 400,
      code: 'VALIDATION_ERROR',
      details: [
        { field: 'name', code: 'VEHICLE_NAME_REQUIRED' },
        { field: 'stops[3].timeWindow.end', code: 'INVALID_TIME_WINDOW' },
        { field: 'capacity', code: 'INVALID_CAPACITY' },
      ],
    });
    expect(fieldErrors(error, { name: 'Tên xe' })).toEqual({
      name: 'Vui lòng nhập tên xe.',
      'stops[3].timeWindow.end': 'Giờ kết thúc phải sau giờ bắt đầu.',
      capacity: 'Sức chứa phải lớn hơn 0.',
    });
  });

  it('đổi giây chờ sang chữ', () => {
    expect(formatWait(45)).toBe('45 giây');
    expect(formatWait(3600)).toBe('1 giờ');
  });
});
