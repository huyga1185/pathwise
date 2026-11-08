/** Query keys of TanStack Query, one place for every resource (docs/client/ARCHITECTURE.md §5). */
export const queryKeys = {
  me: ['me'] as const,
  store: ['store'] as const,
  sessions: ['sessions'] as const,
  vehicles: (filter: { active?: boolean } = {}) => ['vehicles', filter] as const,
  vehiclesAll: ['vehicles'] as const,
  members: ['members'] as const,
  invitations: ['invitations'] as const,
  assignableMembers: ['members', { assignable: true }] as const,
  plans: (filter: { date?: string; status?: string; page: number }) => ['plans', filter] as const,
  plansAll: ['plans'] as const,
  plan: (id: number) => ['plan', id] as const,
};
