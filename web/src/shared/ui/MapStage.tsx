import {
  createContext,
  useContext,
  useLayoutEffect,
  useRef,
  useState,
  type CSSProperties,
  type KeyboardEvent,
  type PointerEvent,
  type ReactNode,
} from 'react';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import { vi } from '@/shared/i18n/vi';
import { useFoldedSidebar } from './AppShell';
import { cx } from './cx';
import { ChevronUpIcon } from './icons';

/** The part of the map the panel or the sheet covers, in px: fitting the routes keeps them clear of it. */
export interface MapInset {
  top: number;
  right: number;
  bottom: number;
  left: number;
}

export type Detent = 'low' | 'mid' | 'high';
const ORDER: Detent[] = ['low', 'mid', 'high'];

/** Panel width on a wide window (OWNER_UI_REDESIGN G5-Q3), its gap to the map's edges. */
const PANEL_WIDTH = 460;
const GAP = 12;

const StageContext = createContext<{ wide: boolean; setDetent: (detent: Detent) => void } | null>(null);
/** Inside a MapStage: whether it shows a panel (wide) and a way to move the phone's sheet (a stop card raises it). */
export function useMapStage() {
  return useContext(StageContext);
}

/**
 * A plan's screens on a map that fills the content area (OWNER_UI_REDESIGN §10, G5.1–G5.3; HIG Maps: "make your map
 * interactive"). Wide window: the content in a panel floating on the leading side (~460px, G5-Q3). Phone: in a
 * nonmodal sheet from the bottom, above the tab bar, with three detents — low (its header, mark it `data-peek`), half,
 * nearly full (G5-Q4; HIG Sheets: detents, "Include a grabber"; a tap on the grabber cycles through them, arrow keys
 * move them, so no dragging is needed — WCAG 2.5.7). Both are Regular glass (owner's choice, LIQUID_GLASS M4/M7).
 * `map` gets the covered inset so the routes are fitted where they can be seen.
 */
export function MapStage({
  label,
  map,
  children,
}: {
  label: string;
  map: (inset: MapInset) => ReactNode;
  children: ReactNode;
}) {
  const wide = useMediaQuery(WIDE_QUERY);
  // Wide: the sidebar folds away while a plan is on the full map, back as it was when the page closes (§10.7).
  useFoldedSidebar(wide);
  const stage = useRef<HTMLDivElement>(null);
  const [height, setHeight] = useState(0);
  const [peek, setPeek] = useState(0);
  // The sheet sits above the floating tab bar: the room it has is the stage less that.
  const [floor, setFloor] = useState(0);
  // Wide: the map fills the window under the sidebar and the top bar; what the panel covers is measured.
  const [covered, setCovered] = useState({ left: PANEL_WIDTH + 2 * GAP, top: 0 });
  const [detent, setDetent] = useState<Detent>('mid');

  // The page coming in may slide (a transform on the page, AppShell pageMotion): a fixed map inside a transformed
  // element is laid out in that element, not the window — it showed small, then jumped to the window and tore. While
  // the map shows, the page's entrance only fades (same timing; a fade is the reduced-motion entrance too, CĐ6).
  useLayoutEffect(() => {
    const page = stage.current?.closest('main');
    page?.getAnimations?.().forEach((animation) => {
      const effect = animation.effect as KeyframeEffect | null;
      effect?.setKeyframes?.([{ opacity: 0 }, { opacity: 1 }]);
    });
  }, []);

  useLayoutEffect(() => {
    const element = stage.current;
    if (!element) return undefined;
    const measure = () => {
      setHeight(element.clientHeight);
      const panel = element.querySelector<HTMLElement>('.pw-map-panel');
      // Measured once laid out (an unrendered panel has no size: keep the panel's own width then).
      if (panel && panel.getBoundingClientRect().width > 0) {
        const box = panel.getBoundingClientRect();
        const stageBox = element.getBoundingClientRect();
        setCovered({ left: Math.round(box.right - stageBox.left + GAP), top: Math.round(box.top - stageBox.top) });
      }
      const sheetBox = element.querySelector<HTMLElement>('.pw-map-sheet');
      setFloor(sheetBox ? parseFloat(getComputedStyle(sheetBox).bottom) || 0 : 0);
      const mark = element.querySelector<HTMLElement>('[data-peek]');
      const sheet = element.querySelector<HTMLElement>('.pw-map-sheet-body');
      if (mark && sheet) setPeek(mark.getBoundingClientRect().bottom - sheet.getBoundingClientRect().top);
    };
    measure();
    if (typeof ResizeObserver === 'undefined') return undefined;
    const observer = new ResizeObserver(measure);
    observer.observe(element);
    const mark = element.querySelector('[data-peek]');
    if (mark) observer.observe(mark);
    return () => observer.disconnect();
  }, [wide]);

  const heights = detentHeights(height - floor, peek);
  const inset: MapInset = wide
    ? { top: covered.top, right: 0, bottom: 0, left: covered.left }
    : { top: 0, right: 0, bottom: heights.mid + floor, left: 0 };

  return (
    <StageContext.Provider value={{ wide, setDetent }}>
      <div
        ref={stage}
        className={cx('pw-map-stage', wide ? 'is-wide' : 'is-narrow')}
        // The map's attribution stays clear of the sheet at its lowest resting height (HIG Maps: "place the… legal
        // link 10 points above the lowest resting position of the card").
        style={wide ? undefined : ({ '--map-floor': `${heights.low + floor + 10}px` } as CSSProperties)}
      >
        <div className="pw-map-stage-map">{map(inset)}</div>
        {wide ? (
          <section className="pw-map-panel" aria-label={label}>
            {children}
          </section>
        ) : (
          <DetentSheet label={label} detent={detent} heights={heights} onDetent={setDetent}>
            {children}
          </DetentSheet>
        )}
      </div>
    </StageContext.Provider>
  );
}

/** Height of the folded sheet: only its handle (owner's sketch fb_14, §10.11). */
const HANDLE = 44;

/** The sheet's resting heights in `height` of room: the handle alone, half, all but a strip of map. */
function detentHeights(height: number, _peek: number): Record<Detent, number> {
  const high = Math.max(0, height - 24);
  const mid = Math.min(high, Math.round(height / 2));
  return { low: HANDLE, mid, high };
}

function DetentSheet({
  label,
  detent,
  heights,
  onDetent,
  children,
}: {
  label: string;
  detent: Detent;
  heights: Record<Detent, number>;
  onDetent: (detent: Detent) => void;
  children: ReactNode;
}) {
  const [drag, setDrag] = useState<number | null>(null);
  const start = useRef<{ y: number; height: number; moved: boolean } | null>(null);
  // A drag ends with a click too: that click must not move the sheet one more detent.
  const dragged = useRef(false);
  const t = vi.mapStage;

  const nearest = (value: number): Detent =>
    ORDER.reduce((best, next) => (Math.abs(heights[next] - value) < Math.abs(heights[best] - value) ? next : best));
  const cycle = () => onDetent(ORDER[(ORDER.indexOf(detent) + 1) % ORDER.length] as Detent);

  const onDown = (event: PointerEvent<HTMLButtonElement>) => {
    start.current = { y: event.clientY, height: heights[detent], moved: false };
    event.currentTarget.setPointerCapture?.(event.pointerId);
  };
  const onMove = (event: PointerEvent<HTMLButtonElement>) => {
    const begun = start.current;
    if (!begun) return;
    const dy = event.clientY - begun.y;
    if (Math.abs(dy) > 4) begun.moved = true;
    if (begun.moved) setDrag(Math.min(heights.high, Math.max(heights.low, begun.height - dy)));
  };
  const onUp = () => {
    const begun = start.current;
    start.current = null;
    dragged.current = Boolean(begun?.moved);
    if (begun?.moved && drag !== null) onDetent(nearest(drag));
    setDrag(null);
  };
  const onKey = (event: KeyboardEvent<HTMLButtonElement>) => {
    const index = ORDER.indexOf(detent);
    if (event.key === 'ArrowUp' && index < ORDER.length - 1) onDetent(ORDER[index + 1] as Detent);
    else if (event.key === 'ArrowDown' && index > 0) onDetent(ORDER[index - 1] as Detent);
    else return;
    event.preventDefault();
  };

  return (
    <section
      className={cx('pw-map-sheet', drag !== null && 'is-dragging')}
      aria-label={label}
      data-detent={detent}
      style={{ height: drag ?? heights[detent] }}
    >
      <button
        type="button"
        className="pw-map-sheet-grabber"
        aria-label={t.resize(t.detents[detent])}
        onClick={() => {
          if (dragged.current) dragged.current = false;
          else cycle();
        }}
        onPointerDown={onDown}
        onPointerMove={onMove}
        onPointerUp={onUp}
        onPointerCancel={onUp}
        onKeyDown={onKey}
      >
        {/* Folded: a chevron up says it opens (fb_14); otherwise the usual grabber bar. */}
        {detent === 'low' && drag === null ? (
          <span className="pw-map-sheet-chevron" aria-hidden="true">
            <ChevronUpIcon />
          </span>
        ) : (
          <span aria-hidden="true" />
        )}
      </button>
      {/* Folded, the content is inert and hidden: Tab and screen readers do not reach what cannot be seen. */}
      <div className="pw-map-sheet-body" inert={detent === 'low' && drag === null}>
        {children}
      </div>
    </section>
  );
}
