import { describe, expect, it } from 'vitest';
import { formatDateLong, formatDistance, formatDuration, formatPhone, formatTime } from './index';

describe('format', () => {
  it('định dạng số điện thoại E.164 của Việt Nam như DESIGN_SYSTEM', () => {
    expect(formatPhone('+84905123456')).toBe('0905 123 456');
    expect(formatPhone('+84905123008')).toBe('0905 123 008');
    expect(formatPhone('+12025550123')).toBe('+12025550123');
    expect(formatPhone(null)).toBe('');
  });

  it('đổi mét sang km một chữ số thập phân, dấu phẩy', () => {
    expect(formatDistance(15366)).toBe('15,4 km');
    expect(formatDistance(800)).toBe('0,8 km');
    expect(formatDistance(null)).toBe('—');
  });

  it('đổi giây sang giờ phút', () => {
    expect(formatDuration(18369)).toBe('5 giờ 06 phút');
    expect(formatDuration(2700)).toBe('45 phút');
  });

  it('hiện giờ theo timezone của store, không theo máy', () => {
    expect(formatTime('2026-12-01T03:39:10Z', 'Asia/Ho_Chi_Minh')).toBe('10:39');
    expect(formatTime('2026-12-01T03:39:10Z', 'UTC')).toBe('03:39');
  });

  it('hiện ngày dài có thứ', () => {
    expect(formatDateLong('2026-12-01')).toBe('Thứ Ba, 01/12/2026');
  });
});
