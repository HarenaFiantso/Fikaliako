import { create } from 'zustand';

import type { AuthSession, UserProfile } from '@fikaliako/api-client';

import { api } from '@/lib/api';

import { AuthError, networkError, problemMessage } from './errors';
import { normalizePhone } from './phone';
import {
  adoptTokens,
  currentRefreshToken,
  dropTokens,
  hydrateTokens,
  onSessionExpired,
} from './token-manager';
import { clearStoredSession, loadStoredSession, saveUser } from './token-storage';

export type SessionStatus = 'restoring' | 'signedOut' | 'signedIn';

interface SignInInput {
  phone: string;
  password: string;
}

interface RegisterInput {
  phone: string;
  password: string;
  displayName: string;
  accountType: 'consumer' | 'business';
}

interface VerifyPhoneInput {
  phone: string;
  code: string;
}

interface ResetPasswordInput {
  phone: string;
  code: string;
  newPassword: string;
}

interface ChangePasswordInput {
  currentPassword: string;
  newPassword: string;
}

interface SessionState {
  status: SessionStatus;
  user: UserProfile | null;
  restore: () => Promise<void>;
  signIn: (input: SignInInput) => Promise<void>;
  register: (input: RegisterInput) => Promise<string>;
  verifyPhone: (input: VerifyPhoneInput) => Promise<void>;
  resendOtp: (phone: string) => Promise<void>;
  forgotPassword: (phone: string) => Promise<void>;
  resetPassword: (input: ResetPasswordInput) => Promise<void>;
  changePassword: (input: ChangePasswordInput) => Promise<void>;
  refreshProfile: () => Promise<void>;
  signOut: () => Promise<void>;
}

function requirePhone(raw: string): string {
  const phone = normalizePhone(raw);
  if (!phone) throw new AuthError('Enter a valid phone number');
  return phone;
}

export const useSession = create<SessionState>((set, get) => {
  async function adoptSession(session: AuthSession): Promise<void> {
    await adoptTokens(session.tokens);
    await saveUser(session.user);
    set({ status: 'signedIn', user: session.user });
  }

  return {
    status: 'restoring',
    user: null,

    async restore() {
      const stored = await loadStoredSession();
      if (!stored) {
        set({ status: 'signedOut', user: null });
        return;
      }
      hydrateTokens(stored.tokens);
      set({ status: 'signedIn', user: stored.user });
      void get().refreshProfile();
    },

    async signIn({ phone, password }) {
      const body = { phone: requirePhone(phone), password };
      let result;
      try {
        result = await api.POST('/v1/auth/login', { body });
      } catch {
        throw networkError();
      }
      const { data, error, response } = result;
      if (data) {
        await adoptSession(data);
        return;
      }
      if (response.status === 401) {
        throw new AuthError('Incorrect phone number or password', 'invalidCredentials');
      }
      if (response.status === 403) {
        const detail = problemMessage(error, '');
        if (/suspend/i.test(detail)) throw new AuthError(detail);
        throw new AuthError('This phone number is not verified yet', 'phoneUnverified');
      }
      throw new AuthError(problemMessage(error, 'Could not sign in'));
    },

    async register({ phone, password, displayName, accountType }) {
      const normalized = requirePhone(phone);
      let result;
      try {
        result = await api.POST('/v1/auth/register', {
          body: {
            phone: normalized,
            password,
            display_name: displayName,
            account_type: accountType,
            locale: 'fr',
          },
        });
      } catch {
        throw networkError();
      }
      const { data, error, response } = result;
      if (data) return normalized;
      if (response.status === 409) {
        throw new AuthError('An account already exists for this number', 'accountExists');
      }
      throw new AuthError(problemMessage(error, 'Could not create the account'));
    },

    async verifyPhone({ phone, code }) {
      const body = { phone: requirePhone(phone), code };
      let result;
      try {
        result = await api.POST('/v1/auth/verify-phone', { body });
      } catch {
        throw networkError();
      }
      const { data, error, response } = result;
      if (data) {
        await adoptSession(data);
        return;
      }
      if (response.status === 400) {
        throw new AuthError('Invalid or expired code', 'invalidCode');
      }
      throw new AuthError(problemMessage(error, 'Could not verify the number'));
    },

    async resendOtp(phone) {
      const body = { phone: requirePhone(phone) };
      let result;
      try {
        result = await api.POST('/v1/auth/resend-otp', { body });
      } catch {
        throw networkError();
      }
      const { error, response } = result;
      if (response.ok) return;
      if (response.status === 429) {
        throw new AuthError('Too many codes requested. Try again later.', 'rateLimited');
      }
      throw new AuthError(problemMessage(error, 'Could not send the code'));
    },

    async forgotPassword(phone) {
      const body = { phone: requirePhone(phone) };
      let result;
      try {
        result = await api.POST('/v1/auth/forgot-password', { body });
      } catch {
        throw networkError();
      }
      const { error, response } = result;
      if (response.ok) return;
      if (response.status === 429) {
        throw new AuthError('Too many codes requested. Try again later.', 'rateLimited');
      }
      throw new AuthError(problemMessage(error, 'Could not send the code'));
    },

    async resetPassword({ phone, code, newPassword }) {
      const body = { phone: requirePhone(phone), code, new_password: newPassword };
      let result;
      try {
        result = await api.POST('/v1/auth/reset-password', { body });
      } catch {
        throw networkError();
      }
      const { error, response } = result;
      if (response.ok) return;
      if (response.status === 400) {
        throw new AuthError('Invalid or expired code', 'invalidCode');
      }
      throw new AuthError(problemMessage(error, 'Could not reset the password'));
    },

    async changePassword({ currentPassword, newPassword }) {
      let result;
      try {
        result = await api.POST('/v1/auth/change-password', {
          body: { current_password: currentPassword, new_password: newPassword },
        });
      } catch {
        throw networkError();
      }
      const { data, error, response } = result;
      if (data) {
        await adoptTokens(data);
        return;
      }
      if (response.status === 400 || response.status === 401) {
        throw new AuthError(problemMessage(error, 'Current password is incorrect'));
      }
      throw new AuthError(problemMessage(error, 'Could not change the password'));
    },

    async refreshProfile() {
      try {
        const { data } = await api.GET('/v1/users/me');
        if (data) {
          await saveUser(data);
          set({ user: data });
        }
      } catch {
        return;
      }
    },

    async signOut() {
      const refreshToken = currentRefreshToken();
      if (refreshToken) {
        try {
          await api.POST('/v1/auth/logout', { body: { refresh_token: refreshToken } });
        } catch {
          // Server-side revocation is best-effort; the local session is dropped regardless.
        }
      }
      dropTokens();
      await clearStoredSession();
      set({ status: 'signedOut', user: null });
    },
  };
});

onSessionExpired(() => {
  void clearStoredSession();
  useSession.setState({ status: 'signedOut', user: null });
});
