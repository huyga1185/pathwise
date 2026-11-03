/** Query keys of TanStack Query, one place for every resource (docs/client/ARCHITECTURE.md §5). */
export const queryKeys = {
  me: ['me'] as const,
  store: ['store'] as const,
  sessions: ['sessions'] as const,
};
