import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
import { useApi } from '@/data/ApiContext';
import { unwrap, unwrapEmpty } from '@/data/http';
import { queryKeys } from '@/data/queryKeys';
import type { Schemas } from '@/data/types';

export type Me = Schemas['GetUserResponse'];
export type DeviceSession = Schemas['SessionResponse'];

/** The signed-in user (`GET /users/me`). */
export function useMe() {
  const api = useApi();
  return useQuery({ queryKey: queryKeys.me, queryFn: () => unwrap(api.GET('/api/v1/users/me')) });
}

/** Signed-in devices of the user (`GET /auth/sessions`). */
export function useDeviceSessions() {
  const api = useApi();
  return useQuery({ queryKey: queryKeys.sessions, queryFn: () => unwrap(api.GET('/api/v1/auth/sessions')) });
}

/** Profile, password and session calls (API_SPEC §2, §3). Each throws an ApiError for an error response. */
export function useProfileApi() {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMemo(() => {
    const reloadSessions = () => queryClient.invalidateQueries({ queryKey: queryKeys.sessions });
    return {
      updateMe: async (body: Schemas['UpdateProfileRequest']) => {
        const me = await unwrap(api.PATCH('/api/v1/users/me', { body }));
        queryClient.setQueryData(queryKeys.me, me);
        return me;
      },
      requestPasswordOtp: (currentPassword: string) =>
        unwrapEmpty(api.POST('/api/v1/auth/password/change/otp', { body: { currentPassword } })),
      changePassword: async (body: Schemas['ChangePasswordRequest']) => {
        await unwrapEmpty(api.PUT('/api/v1/auth/password', { body }));
        await reloadSessions();
      },
      revokeSession: async (sessionId: string) => {
        try {
          await unwrapEmpty(api.DELETE('/api/v1/auth/sessions/{sessionId}', { params: { path: { sessionId } } }));
        } finally {
          await reloadSessions();
        }
      },
      revokeOtherSessions: async () => {
        await unwrapEmpty(api.POST('/api/v1/auth/sessions/revoke-others'));
        await reloadSessions();
      },
    };
  }, [api, queryClient]);
}
