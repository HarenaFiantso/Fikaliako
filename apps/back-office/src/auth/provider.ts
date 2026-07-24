import type { UserRole } from '@fikaliako/api-client';
import type { AuthProvider } from 'react-admin';

import { api } from '../lib/api';
import { problemMessage } from '../lib/problem';
import { clearSession, loadSession, saveSession } from './session-store';

/** Only back-office roles may enter (project book ch. 7.3). */
const BACK_OFFICE_ROLES: UserRole[] = ['moderator', 'admin'];

export type LoginParams = {
  phone: string;
  password: string;
};

export const authProvider: AuthProvider = {
  async login(params) {
    const { phone, password } = params as LoginParams;
    const { data, error } = await api.POST('/v1/auth/login', { body: { phone, password } });
    if (error || !data) {
      throw new Error(problemMessage(error, 'Sign-in failed. Check your phone number and password.'));
    }
    if (!BACK_OFFICE_ROLES.includes(data.user.role)) {
      // Valid account, wrong tree: revoke the session we just opened.
      void api.POST('/v1/auth/logout', { body: { refresh_token: data.tokens.refresh_token } });
      throw new Error('This account has no back-office access.');
    }
    saveSession(data.user, data.tokens);
  },

  async logout() {
    const session = loadSession();
    if (session) {
      // Best effort: the server revokes the session, but we sign out locally anyway.
      await api
        .POST('/v1/auth/logout', { body: { refresh_token: session.refreshToken } })
        .catch(() => undefined);
    }
    clearSession();
  },

  async checkAuth() {
    const session = loadSession();
    if (!session || !BACK_OFFICE_ROLES.includes(session.user.role)) {
      throw new Error('Please sign in.');
    }
  },

  async checkError(error) {
    // The token manager already retries expired tokens; a surviving 401 means
    // the session is truly gone. 403s are surfaced, not treated as sign-outs.
    const status = (error as { status?: number } | undefined)?.status;
    if (status === 401) {
      clearSession();
      throw new Error('Session expired. Please sign in again.');
    }
  },

  async getPermissions() {
    return loadSession()?.user.role ?? null;
  },

  async getIdentity() {
    const session = loadSession();
    if (!session) throw new Error('Not signed in.');
    return { id: session.user.id, fullName: session.user.display_name };
  },
};
