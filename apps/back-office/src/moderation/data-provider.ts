import type { ContributionStatus, Problem } from '@fikaliako/api-client';
import {
  HttpError,
  type DataProvider,
  type GetListParams,
  type GetListResult,
  type GetOneParams,
  type GetOneResult,
} from 'react-admin';

import { api } from '../lib/api';
import { problemMessage } from '../lib/problem';
import type { ContributionRecord } from './types';

export interface ModerationDataProvider extends DataProvider {
  approve(id: string): Promise<ContributionRecord>;
  reject(id: string, reason?: string): Promise<ContributionRecord>;
}

const DEFAULT_STATUS: ContributionStatus = 'pending';

/**
 * The API pages with opaque cursors while react-admin asks for page numbers,
 * so remember the cursor that opens each page as pages are visited (the UI
 * only navigates next/previous, making sequential access a given).
 */
const cursors = new Map<string, string>();

const cursorKey = (status: string, perPage: number, page: number) =>
  `${status}|${perPage}|${page}`;

function toError(problem: Problem | undefined, response: Response, fallback: string): HttpError {
  return new HttpError(problemMessage(problem, fallback), response.status, problem);
}

function assertContributions(resource: string): void {
  if (resource !== 'contributions') {
    throw new Error(`Unknown resource '${resource}'.`);
  }
}

async function getList(
  resource: string,
  params: GetListParams,
): Promise<GetListResult<ContributionRecord>> {
  assertContributions(resource);
  const page = params.pagination?.page ?? 1;
  const perPage = params.pagination?.perPage ?? 20;
  const filter = params.filter as { status?: ContributionStatus } | undefined;
  const status = filter?.status ?? DEFAULT_STATUS;
  const cursor = page > 1 ? cursors.get(cursorKey(status, perPage, page)) : undefined;

  const { data, error, response } = await api.GET('/v1/moderation/contributions', {
    params: { query: { status, limit: perPage, ...(cursor ? { cursor } : {}) } },
  });
  if (error || !data) throw toError(error, response, 'Could not load the moderation queue.');

  if (data.next_cursor) cursors.set(cursorKey(status, perPage, page + 1), data.next_cursor);
  return {
    data: data.items,
    pageInfo: {
      hasNextPage: Boolean(data.next_cursor),
      hasPreviousPage: page > 1,
    },
  };
}

async function getOne(
  resource: string,
  params: GetOneParams,
): Promise<GetOneResult<ContributionRecord>> {
  assertContributions(resource);
  const { data, error, response } = await api.GET('/v1/moderation/contributions/{contributionId}', {
    params: { path: { contributionId: String(params.id) } },
  });
  if (error || !data) throw toError(error, response, 'Could not load the contribution.');
  return { data };
}

async function approve(id: string): Promise<ContributionRecord> {
  const { data, error, response } = await api.POST(
    '/v1/moderation/contributions/{contributionId}/approve',
    { params: { path: { contributionId: id } } },
  );
  if (error || !data) throw toError(error, response, 'Could not approve the contribution.');
  return data;
}

async function reject(id: string, reason?: string): Promise<ContributionRecord> {
  const trimmed = reason?.trim();
  const { data, error, response } = await api.POST(
    '/v1/moderation/contributions/{contributionId}/reject',
    {
      params: { path: { contributionId: id } },
      body: trimmed ? { reason: trimmed } : {},
    },
  );
  if (error || !data) throw toError(error, response, 'Could not reject the contribution.');
  return data;
}

const unsupported = async (): Promise<never> => {
  throw new Error('The moderation queue is read/approve/reject only.');
};

const provider = {
  getList,
  getOne,
  getMany: unsupported,
  getManyReference: unsupported,
  create: unsupported,
  update: unsupported,
  updateMany: unsupported,
  delete: unsupported,
  deleteMany: unsupported,
  approve,
  reject,
};

export const dataProvider = provider as unknown as ModerationDataProvider;
