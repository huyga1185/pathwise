import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import { statusInfo, type StatusKind, type Tone } from './index';

const TONE_OF_TOKEN: Record<string, Tone> = {
  'ink-muted': 'muted',
  info: 'info',
  signal: 'signal',
  danger: 'danger',
  success: 'success',
};

/** Rows of the status table in docs/client/DESIGN_SYSTEM.md: label(s) and token(s). */
function designSystemRows(): Array<{ labels: string[]; tones: Tone[] }> {
  const doc = readFileSync(resolve(__dirname, '../../../../docs/client/DESIGN_SYSTEM.md'), 'utf8');
  const table = doc.slice(
    doc.indexOf('| Trạng thái (API) |'),
    doc.indexOf('\n\n', doc.indexOf('| Trạng thái (API) |')),
  );
  return table
    .split('\n')
    .slice(2)
    .map((line) => line.split('|').map((cell) => cell.trim()))
    .map((cells) => ({
      labels: (cells[2] ?? '').split(' / ').map((label) => label.trim()),
      tones: [...(cells[3] ?? '').matchAll(/`([a-z-]+)`/g)].map((m) => TONE_OF_TOKEN[m[1] ?? ''] as Tone),
    }));
}

const ALL: Record<StatusKind, string[]> = {
  plan: ['DRAFT', 'PUBLISHED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'],
  route: ['PENDING', 'IN_PROGRESS', 'COMPLETED'],
  stop: ['PENDING', 'DELIVERED', 'FAILED', 'SKIPPED'],
  dropReason: ['NOT_OPTIMIZED', 'INFEASIBLE', 'MANUAL'],
  geocode: ['PENDING', 'FAILED'],
  processing: ['GEOCODING', 'OPTIMIZING'],
  invitation: ['PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED'],
  member: ['PENDING_VERIFICATION', 'ACTIVE', 'DISABLED'],
  vehicle: ['ACTIVE', 'INACTIVE'],
  job: ['QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED'],
};

describe('statusInfo', () => {
  it('mỗi nhãn trong bảng trạng thái của DESIGN_SYSTEM có đúng tone', () => {
    const produced = Object.entries(ALL).flatMap(([kind, statuses]) =>
      statuses.map((status) => statusInfo(kind as StatusKind, status)).filter((info) => info !== null),
    );
    const rows = designSystemRows();
    expect(rows.length).toBeGreaterThan(10);
    rows.forEach(({ labels, tones }) => {
      labels.forEach((label, index) => {
        const tone = tones[Math.min(index, tones.length - 1)];
        expect(produced, `${label} (${tone})`).toContainEqual({ label, tone });
      });
    });
  });

  it('không có pill cho trạng thái không cần hiện', () => {
    expect(statusInfo('geocode', 'RESOLVED')).toBeNull();
    expect(statusInfo('geocode', 'PROVIDED')).toBeNull();
    expect(statusInfo('processing', 'IDLE')).toBeNull();
    expect(statusInfo('plan', undefined)).toBeNull();
  });

  it('cùng một mã có nhãn khác nhau theo loại', () => {
    expect(statusInfo('stop', 'PENDING')).toEqual({ label: 'CHỜ GIAO', tone: 'muted' });
    expect(statusInfo('geocode', 'PENDING')).toEqual({ label: 'ĐANG TÌM TOẠ ĐỘ', tone: 'info' });
    expect(statusInfo('invitation', 'PENDING')).toEqual({ label: 'CHỜ NHẬN', tone: 'info' });
  });
});
