import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { blankBasemap } from './basemap';
import { BasemapProvider } from './BasemapContext';
import { RouteMap } from './RouteMap';

const labels = {
  zoomIn: 'Phóng to bản đồ',
  zoomOut: 'Thu nhỏ bản đồ',
  depot: 'Kho xuất phát',
  expand: 'Mở rộng bản đồ',
  collapse: 'Thu nhỏ khung',
  legend: 'Chú giải bản đồ',
};

describe('RouteMap', () => {
  it('marker đánh số theo màu tuyến, chú giải có kho và tên tài xế, bấm marker thì chọn điểm', async () => {
    const onSelect = vi.fn();
    const { container } = render(
      <BasemapProvider basemap={blankBasemap('Bản đồ thử')}>
        <RouteMap
          depot={{ lat: 16.06, lon: 108.21 }}
          routes={[
            { key: 1, label: 'Tài xế A', stops: [{ id: 'a1', lat: 16.07, lon: 108.22, sequence: 1, label: 'Điểm 1' }] },
            {
              key: 2,
              label: 'Tài xế B',
              stops: [{ id: 'b1', lat: 16.05, lon: 108.2, sequence: 1, tone: 'success', label: 'Điểm 1 B' }],
            },
          ]}
          unassigned={[{ id: 'u1', lat: 16.08, lon: 108.23, tone: 'signal', label: 'Không xếp được' }]}
          selectedId="a1"
          onSelect={onSelect}
          labels={labels}
        />
      </BasemapProvider>,
    );
    const user = userEvent.setup();

    const legend = screen.getByRole('list', { name: 'Chú giải bản đồ' });
    expect(
      within(legend)
        .getAllByRole('listitem')
        .map((item) => item.textContent),
    ).toEqual(['Kho xuất phát', 'Tài xế A', 'Tài xế B']);
    expect(container.querySelector('[title="Điểm 1"] .pw-marker-r1.pw-marker-selected')).toHaveTextContent('1');
    expect(container.querySelector('[title="Điểm 1 B"] .pw-marker-success')).toBeInTheDocument();
    expect(container.querySelector('[title="Không xếp được"] .pw-marker-signal')).toHaveTextContent('');
    expect(container.querySelectorAll('path.pw-route-line')).toHaveLength(2);
    expect(screen.getByText('Bản đồ thử')).toBeInTheDocument();

    // Markers are reachable by keyboard (Leaflet: focusable icon, Enter = click).
    const marker = container.querySelector('[title="Điểm 1 B"]') as HTMLElement;
    expect(marker).toHaveAttribute('tabindex', '0');
    fireEvent.keyPress(marker, { key: 'Enter', code: 'Enter', keyCode: 13, charCode: 13 });
    expect(onSelect).toHaveBeenCalledWith('b1');

    await user.click(screen.getByRole('button', { name: 'Mở rộng bản đồ' }));
    expect(screen.getByRole('button', { name: 'Thu nhỏ khung' })).toHaveAttribute('aria-pressed', 'true');
  });
});
