import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
import { useApi } from '@/data/ApiContext';
import { unwrap, unwrapEmpty } from '@/data/http';
import { queryKeys } from '@/data/queryKeys';
import type { Schemas } from '@/data/types';

export type Member = Schemas['MemberResponse'];
export type Invitation = Schemas['InvitationResponse'];
export type InvitableRole = Schemas['CreateInvitationRequest']['role'];

/** Members of the store (`GET /store/members`). */
export function useMembers() {
  const api = useApi();
  return useQuery({ queryKey: queryKeys.members, queryFn: () => unwrap(api.GET('/api/v1/store/members')) });
}

/** Invitations of the store (`GET /store/invitations`), OWNER only: not loaded for anyone else. */
export function useInvitations(enabled: boolean) {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.invitations,
    queryFn: () => unwrap(api.GET('/api/v1/store/invitations')),
    enabled,
  });
}

/**
 * Changes to members and invitations (API_SPEC §4), OWNER only. Each throws an ApiError. A changed member replaces its
 * row with what the server returned; invitations are reloaded.
 */
export function useMemberApi() {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMemo(() => {
    const replace = (member: Member) =>
      queryClient.setQueryData<Member[]>(queryKeys.members, (list) =>
        list?.map((item) => (item.id === member.id ? member : item)),
      );
    const reloadInvitations = () => queryClient.invalidateQueries({ queryKey: queryKeys.invitations });
    return {
      updateMember: async (userId: number, body: Schemas['UpdateMemberRequest']) => {
        replace(await unwrap(api.PATCH('/api/v1/store/members/{userId}', { params: { path: { userId } }, body })));
      },
      /** The OWNER turning their own "Đi giao" on or off. */
      updateOwnCanDrive: async (canDrive: boolean) => {
        replace(await unwrap(api.PATCH('/api/v1/store/members/me/can-drive', { body: { canDrive } })));
      },
      invite: async (body: Schemas['CreateInvitationRequest']) => {
        const invitation = await unwrap(api.POST('/api/v1/store/invitations', { body }));
        await reloadInvitations();
        return invitation;
      },
      revokeInvitation: async (id: number) => {
        try {
          await unwrapEmpty(api.POST('/api/v1/store/invitations/{id}/revoke', { params: { path: { id } } }));
        } finally {
          await reloadInvitations();
        }
      },
    };
  }, [api, queryClient]);
}
