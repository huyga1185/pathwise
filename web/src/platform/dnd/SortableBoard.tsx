import {
  closestCorners,
  DndContext,
  KeyboardSensor,
  PointerSensor,
  useDroppable,
  useSensor,
  useSensors,
  type Announcements,
  type DragEndEvent,
  type DragOverEvent,
  type DragStartEvent,
  type UniqueIdentifier,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { useState, type HTMLAttributes, type ReactNode } from 'react';

/**
 * Drag-and-drop port (docs/client/ARCHITECTURE.md §2.1, P-58): lists of items in columns, reordered or moved between
 * columns with the mouse, touch or the keyboard (Space lifts and drops, arrows move, Escape cancels). Only this
 * folder knows `@dnd-kit`; features pass ids and render their own rows.
 */

/** Item ids by column id; the order of the keys is the order of the columns. */
export type BoardItems = Record<string, string[]>;

/** Spread on the element the user grabs (a button): pointer and keyboard listeners, ARIA attributes. */
export type DragHandleProps = HTMLAttributes<HTMLElement>;

export interface BoardTexts {
  /** Read by screen readers when an item gets focus: how to drag with the keyboard. */
  instructions: string;
  picked: (item: string, column: string, position: number) => string;
  over: (item: string, column: string, position: number) => string;
  dropped: (item: string, column: string, position: number) => string;
  cancelled: (item: string) => string;
}

export interface SortableBoardProps {
  items: BoardItems;
  onChange: (items: BoardItems) => void;
  /** Name of a column and of an item, for the screen reader sentences. */
  columnLabel: (columnId: string) => string;
  itemLabel: (itemId: string) => string;
  /** A column around its list; `list` holds the items and accepts drops even when empty. */
  renderColumn: (columnId: string, list: ReactNode) => ReactNode;
  /** One item; `handle` goes on the grab button, `dragging` while it is lifted. */
  renderItem: (itemId: string, columnId: string, handle: DragHandleProps, dragging: boolean) => ReactNode;
  texts: BoardTexts;
}

export function SortableBoard({
  items,
  onChange,
  columnLabel,
  itemLabel,
  renderColumn,
  renderItem,
  texts,
}: SortableBoardProps) {
  const sensors = useSensors(
    // A small move before a drag starts, so a click on the handle stays a click.
    useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );
  const [active, setActive] = useState<{ id: string; from: BoardItems } | null>(null);

  const columnOf = (id: UniqueIdentifier, board: BoardItems = items): string | undefined => {
    const key = String(id);
    if (key in board) return key;
    return Object.keys(board).find((column) => board[column]?.includes(key));
  };
  const place = (id: UniqueIdentifier, board: BoardItems = items) => {
    const column = columnOf(id, board);
    if (column === undefined) return null;
    const index = board[column]?.indexOf(String(id)) ?? -1;
    return { column, position: index < 0 ? (board[column]?.length ?? 0) + 1 : index + 1 };
  };

  const announcements: Announcements = {
    onDragStart: ({ active: item }) => {
      const at = place(item.id);
      return at ? texts.picked(itemLabel(String(item.id)), columnLabel(at.column), at.position) : undefined;
    },
    onDragOver: ({ active: item }) => {
      const at = place(item.id);
      return at ? texts.over(itemLabel(String(item.id)), columnLabel(at.column), at.position) : undefined;
    },
    onDragEnd: ({ active: item }) => {
      const at = place(item.id);
      return at ? texts.dropped(itemLabel(String(item.id)), columnLabel(at.column), at.position) : undefined;
    },
    onDragCancel: ({ active: item }) => texts.cancelled(itemLabel(String(item.id))),
  };

  const onDragStart = (event: DragStartEvent) => setActive({ id: String(event.active.id), from: items });

  // Moving into another column happens while dragging, so the item shows where it would land.
  const onDragOver = ({ active: item, over }: DragOverEvent) => {
    if (!over) return;
    const from = columnOf(item.id);
    const to = columnOf(over.id);
    if (from === undefined || to === undefined || from === to) return;
    const source = (items[from] ?? []).filter((id) => id !== String(item.id));
    const target = [...(items[to] ?? [])];
    const overIndex = target.indexOf(String(over.id));
    target.splice(overIndex < 0 ? target.length : overIndex, 0, String(item.id));
    onChange({ ...items, [from]: source, [to]: target });
  };

  const onDragEnd = ({ active: item, over }: DragEndEvent) => {
    setActive(null);
    if (!over) return;
    const column = columnOf(item.id);
    if (column === undefined || columnOf(over.id) !== column) return;
    const list = items[column] ?? [];
    const from = list.indexOf(String(item.id));
    const to = list.indexOf(String(over.id));
    if (from >= 0 && to >= 0 && from !== to) onChange({ ...items, [column]: arrayMove(list, from, to) });
  };

  const onDragCancel = () => {
    if (active) onChange(active.from);
    setActive(null);
  };

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCorners}
      accessibility={{ announcements, screenReaderInstructions: { draggable: texts.instructions } }}
      onDragStart={onDragStart}
      onDragOver={onDragOver}
      onDragEnd={onDragEnd}
      onDragCancel={onDragCancel}
    >
      {Object.keys(items).map((columnId) => (
        <Column key={columnId} id={columnId} ids={items[columnId] ?? []}>
          {(list) => renderColumn(columnId, list)}
          {(itemId, handle, dragging) => renderItem(itemId, columnId, handle, dragging)}
        </Column>
      ))}
    </DndContext>
  );
}

function Column({
  id,
  ids,
  children: [column, item],
}: {
  id: string;
  ids: string[];
  children: [(list: ReactNode) => ReactNode, (itemId: string, handle: DragHandleProps, dragging: boolean) => ReactNode];
}) {
  // An empty column is a drop target of its own, so it can take items. Only then: a column target over its items
  // would win the collision and swallow every reorder within the column.
  const { setNodeRef } = useDroppable({ id, disabled: ids.length > 0 });
  const list = (
    <SortableContext id={id} items={ids} strategy={verticalListSortingStrategy}>
      <div ref={setNodeRef} className="pw-dnd-list">
        {ids.map((itemId) => (
          <Item key={itemId} id={itemId} render={item} />
        ))}
      </div>
    </SortableContext>
  );
  return <>{column(list)}</>;
}

function Item({
  id,
  render,
}: {
  id: string;
  render: (itemId: string, handle: DragHandleProps, dragging: boolean) => ReactNode;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id });
  return (
    <div
      ref={setNodeRef}
      className="pw-dnd-item"
      style={{ transform: CSS.Transform.toString(transform), transition }}
      data-dragging={isDragging || undefined}
    >
      {render(id, { ...attributes, ...listeners } as DragHandleProps, isDragging)}
    </div>
  );
}
