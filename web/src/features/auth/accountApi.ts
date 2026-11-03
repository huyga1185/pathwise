import { useMemo } from 'react';
import { useApi } from '@/data/ApiContext';
import { unwrap, unwrapEmpty } from '@/data/http';
import type { Schemas } from '@/data/types';

/** Calls of the public account screens (API_SPEC §2, §4). Each throws an ApiError for an error response. */
export function useAccountApi() {
  const api = useApi();
  return useMemo(
    () => ({
      register: (body: Schemas['RegisterRequest']) => unwrap(api.POST('/api/v1/auth/register', { body })),
      verifyEmail: (body: Schemas['VerifyEmailRequest']) =>
        unwrapEmpty(api.POST('/api/v1/auth/email/verify', { body })),
      resendVerification: (email: string) =>
        unwrapEmpty(api.POST('/api/v1/auth/email/verify/resend', { body: { email } })),
      forgotPassword: (email: string) => unwrapEmpty(api.POST('/api/v1/auth/password/forgot', { body: { email } })),
      verifyResetOtp: (body: Schemas['VerifyResetOtpRequest']) =>
        unwrap(api.POST('/api/v1/auth/password/reset/verify', { body })),
      resetPassword: (body: Schemas['ResetPasswordRequest']) =>
        unwrapEmpty(api.POST('/api/v1/auth/password/reset', { body })),
      previewInvitation: (code: string) =>
        unwrap(api.GET('/api/v1/invitations/{code}', { params: { path: { code } } })),
      acceptInvitation: (body: Schemas['AcceptInvitationRequest']) =>
        unwrapEmpty(api.POST('/api/v1/invitations/accept', { body })),
    }),
    [api],
  );
}
