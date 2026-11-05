import { describe, expect, it } from 'vitest';
import { sample } from '@/test/samples';
import type { PlanDetail } from './planApi';
import {
  demandUnits,
  emptyStop,
  formPath,
  planToValues,
  validatePlan,
  valuesToRequest,
  type PlanFormValues,
} from './planForm';

const values = (changes: Partial<PlanFormValues> = {}): PlanFormValues => ({
  name: 'Giao sáng',
  planDate: '2026-12-01',
  assignments: [{ driverId: '451', vehicleId: '74', shiftStart: '08:00', shiftEnd: '17:00', maxDistanceKm: '80' }],
  stops: [{ ...emptyStop(), label: 'ĐH-1', address: '12 Lê Duẩn', demands: { KG: '', BOX: '2', PACKAGE: '' } }],
  ...changes,
});

describe('planForm', () => {
  it('dựng body đúng API_SPEC: km → mét, bỏ ô trống, demand theo loại', () => {
    expect(valuesToRequest(values())).toEqual({
      name: 'Giao sáng',
      planDate: '2026-12-01',
      assignments: [{ driverId: 451, vehicleId: 74, shiftStart: '08:00', shiftEnd: '17:00', maxDistanceMeters: 80000 }],
      stops: [
        {
          id: undefined,
          label: 'ĐH-1',
          recipientName: undefined,
          recipientPhone: undefined,
          address: '12 Lê Duẩn',
          lat: undefined,
          lon: undefined,
          demands: { BOX: 2 },
          serviceTimeSeconds: undefined,
          timeWindow: undefined,
          note: undefined,
        },
      ],
    });
  });

  it('sửa plan: giữ id điểm giao; toạ độ nhập sẵn chỉ gửi lại khi địa chỉ không đổi', () => {
    const plan = sample<{ data: PlanDetail }>('plan-detail-geocoded').data;
    const form = planToValues(plan);
    expect(form.assignments).toHaveLength(2);
    expect(form.stops[0]?.id).toBe(108);
    const provided = form.stops.find((stop) => stop.providedAt !== null);
    expect(provided).toBeDefined();
    const kept = valuesToRequest(form).stops?.find((stop) => stop.id === provided?.id);
    expect(kept?.lat).toBe(provided?.providedAt?.lat);
    const resolved = valuesToRequest(form).stops?.[0];
    expect(resolved?.lat).toBeUndefined();

    if (provided) provided.address = 'Địa chỉ mới';
    expect(valuesToRequest(form).stops?.find((stop) => stop.id === provided?.id)?.lat).toBeUndefined();
  });

  it('đổi đường dẫn lỗi của server sang tên ô của form', () => {
    expect(formPath('stops[3].timeWindow.end')).toBe('stops.3.timeWindow.end');
    expect(formPath('assignments[0].driverId')).toBe('assignments.0.driverId');
    expect(formPath('name')).toBe('name');
  });

  it('kiểm tức thời: mã đơn, địa chỉ hoặc toạ độ, khung giờ, ca, quãng đường', () => {
    const errors = validatePlan(
      values({
        assignments: [{ driverId: '', vehicleId: '74', shiftStart: '17:00', shiftEnd: '08:00', maxDistanceKm: '0' }],
        stops: [{ ...emptyStop(), timeWindow: { start: '11:00', end: '09:00' } }],
      }),
      { driverId: 'Tài xế', label: 'Mã đơn', address: 'Địa chỉ', maxDistanceKm: 'Quãng đường tối đa (km)' },
    );
    expect(errors).toEqual({
      'assignments.0.driverId': 'Vui lòng chọn tài xế.',
      'assignments.0.shiftEnd': 'Giờ kết thúc ca phải sau giờ bắt đầu.',
      'assignments.0.maxDistanceKm': 'Quãng đường tối đa phải lớn hơn 0.',
      'stops.0.label': 'Vui lòng nhập mã đơn.',
      'stops.0.address': 'Nhập địa chỉ hoặc đặt ghim trên bản đồ.',
      'stops.0.timeWindow.end': 'Giờ kết thúc phải sau giờ bắt đầu.',
    });
  });

  it('cột số lượng theo đơn vị của xe đã chọn, bỏ ORDER, thêm đơn vị điểm giao đang có', () => {
    expect(demandUnits(['BOX', 'ORDER'], [])).toEqual(['BOX']);
    expect(demandUnits([], [{ ...emptyStop(), demands: { KG: '5', BOX: '', PACKAGE: '' } }])).toEqual(['KG']);
  });
});
