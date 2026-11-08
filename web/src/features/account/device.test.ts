import { describe, expect, it } from 'vitest';
import { deviceName } from './device';

describe('deviceName', () => {
  it.each([
    [
      'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36',
      'Chrome trên Linux',
    ],
    [
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0 Safari/537.36 Edg/141.0',
      'Edge trên Windows',
    ],
    ['Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5; rv:132.0) Gecko/20100101 Firefox/132.0', 'Firefox trên macOS'],
    [
      'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1',
      'Safari trên iOS',
    ],
    ['Dart/3.5 (dart:io)', 'App PathWise'],
  ])('đặt tên dễ đọc cho %s', (userAgent, name) => {
    expect(deviceName(userAgent, 'Thiết bị không rõ')).toBe(name);
  });

  it('không có hoặc không nhận ra user agent thì dùng tên dự phòng', () => {
    expect(deviceName(null, 'Thiết bị không rõ')).toBe('Thiết bị không rõ');
    expect(deviceName('curl/8.9.1', 'Thiết bị không rõ')).toBe('Thiết bị không rõ');
  });
});
