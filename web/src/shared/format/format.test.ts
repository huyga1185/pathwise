import { describe, expect, it } from 'vitest';
import {
  formatClock,
  formatDate,
  formatDateLong,
  formatDayTime,
  formatDistance,
  formatDuration,
  formatPhone,
  formatTime,
} from './index';

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

  it('định dạng ngày giờ ngắn theo timezone, ngày và giờ chờ trên nút', () => {
    expect(formatDayTime('2026-11-03T17:20:00Z', 'Asia/Ho_Chi_Minh')).toBe('04/11 00:20');
    expect(formatDate('2026-11-10T03:00:00Z', 'Asia/Ho_Chi_Minh')).toBe('10/11/2026');
    expect(formatClock(42)).toBe('0:42');
    expect(formatClock(60)).toBe('1:00');
  });
});
