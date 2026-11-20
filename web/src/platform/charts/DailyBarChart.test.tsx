import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { DailyBarChart } from './DailyBarChart';

const data = [
  { label: '01/12', spoken: 'Thứ Hai, 1 tháng 12, 2026', delivered: 30, failed: 2 },
  { label: '02/12', spoken: 'Thứ Ba, 2 tháng 12, 2026', delivered: 28, failed: 1 },
];

describe('DailyBarChart (G6 §11)', () => {
  it('chú giải có chữ; mỗi ngày đọc được với ngày đầy đủ; mũi tên chọn ngày và dòng tóm tắt đổi theo', async () => {
    render(
      <DailyBarChart
        data={data}
        stacked
        series={[
          { key: 'delivered', label: 'đã giao', token: 'success' },
          { key: 'failed', label: 'không giao được', token: 'danger' },
        ]}
        ariaLabel="Kết quả theo ngày"
        summary={(index) => (index === null ? 'Cả khoảng: 58 đã giao' : `Ngày ${data[index]?.label}`)}
      />,
    );
    const user = userEvent.setup();
    expect(screen.getByText('Thứ Hai, 1 tháng 12, 2026: 30 đã giao, 2 không giao được')).toBeInTheDocument();
    expect(screen.getByText('Cả khoảng: 58 đã giao')).toBeInTheDocument();

    const plot = screen.getByRole('slider', { name: 'Kết quả theo ngày' });
    plot.focus();
    await user.keyboard('{ArrowRight}');
    expect(plot).toHaveAttribute('aria-valuetext', 'Thứ Hai, 1 tháng 12, 2026: 30 đã giao, 2 không giao được');
    expect(screen.getByText('Ngày 01/12')).toBeInTheDocument();
    await user.keyboard('{End}');
    expect(plot).toHaveAttribute('aria-valuenow', '2');
    await user.keyboard('{Escape}');
    expect(screen.getByText('Cả khoảng: 58 đã giao')).toBeInTheDocument();
  });
});
