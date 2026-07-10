'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';

import type {
  ForgotPasswordRequest,
  LoginRequest,
  RegisterRequest,
  ResendOtpRequest,
  ResetPasswordRequest,
  VerifyPhoneRequest,
} from '@fikaliako/api-client';

import { api } from '@/lib/api/client';
import { unwrap } from '@/lib/api/unwrap';
import { useAuthStore } from '@/lib/auth/auth-store';

export function useLogin() {
  return useMutation({
    mutationFn: async (body: LoginRequest) => unwrap(await api.POST('/v1/auth/login', { body })),
    onSuccess: (session) => useAuthStore.getState().setSession(session),
  });
}

export function useRegister() {
  return useMutation({
    mutationFn: async (body: RegisterRequest) =>
      unwrap(await api.POST('/v1/auth/register', { body })),
  });
}

export function useVerifyPhone() {
  return useMutation({
    mutationFn: async (body: VerifyPhoneRequest) =>
      unwrap(await api.POST('/v1/auth/verify-phone', { body })),
    onSuccess: (session) => useAuthStore.getState().setSession(session),
  });
}

export function useResendOtp() {
  return useMutation({
    mutationFn: async (body: ResendOtpRequest) =>
      unwrap(await api.POST('/v1/auth/resend-otp', { body })),
  });
}

export function useForgotPassword() {
  return useMutation({
    mutationFn: async (body: ForgotPasswordRequest) =>
      unwrap(await api.POST('/v1/auth/forgot-password', { body })),
  });
}

export function useResetPassword() {
  return useMutation({
    mutationFn: async (body: ResetPasswordRequest) =>
      unwrap(await api.POST('/v1/auth/reset-password', { body })),
  });
}

/**
 * Revokes the session server-side when possible, but always clears the
 * local session and cached queries — logout must never fail locally.
 */
export function useLogout() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      const { refreshToken } = useAuthStore.getState();
      if (!refreshToken) return;
      try {
        await api.POST('/v1/auth/logout', { body: { refresh_token: refreshToken } });
      } catch {
        // Offline or server error: the local session is dropped regardless.
      }
    },
    onSettled: () => {
      useAuthStore.getState().clearSession();
      queryClient.clear();
    },
  });
}
