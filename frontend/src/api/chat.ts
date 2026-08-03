/**
 * Chat API — wrappers for /chat/* endpoints.
 *
 * DTOs mirror docs/03_API_CONTRACT.yaml entries:
 *   ChatSessionResponse, ChatSessionPageResponse, ChatMessageResponse,
 *   ChatMessagePageResponse, CreateChatSessionRequest, SendMessageRequest.
 *
 * Pages use ChatApi through AuthContext (chatApi getter). The session id
 * lives in sessionStorage, so a page reload re-opens the same session
 * and the UI re-fetches the message history from the backend.
 */

import { ApiClient } from './client';

export type ChatSessionStatus = 'ACTIVE' | 'CLOSED' | 'ARCHIVED';
export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM';
export type AnalysisStatus = 'NOT_REQUESTED' | 'PENDING' | 'SUCCEEDED' | 'FAILED';

export interface ChatSessionResponse {
  id: string;
  title: string | null;
  status: ChatSessionStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessageResponse {
  id: string;
  sessionId: string;
  role: MessageRole;
  content: string;
  createdAt: string;
  analysisStatus: AnalysisStatus;
}

export interface ChatSessionPageResponse {
  items: ChatSessionResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ChatMessagePageResponse {
  items: ChatMessageResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateChatSessionRequest {
  title?: string | null;
}

export interface SendMessageRequest {
  content: string;
}

export class ChatApi {
  constructor(private readonly client: ApiClient) {}

  listSessions(page = 0, size = 20): Promise<ChatSessionPageResponse> {
    return this.client.request<ChatSessionPageResponse>('/chat/sessions', {
      method: 'GET',
      query: { page, size },
    });
  }

  createSession(payload: CreateChatSessionRequest = {}): Promise<ChatSessionResponse> {
    return this.client.request<ChatSessionResponse>('/chat/sessions', {
      method: 'POST',
      body: payload,
    });
  }

  listMessages(sessionId: string, page = 0, size = 20): Promise<ChatMessagePageResponse> {
    return this.client.request<ChatMessagePageResponse>(
      `/chat/sessions/${sessionId}/messages`,
      { method: 'GET', query: { page, size } },
    );
  }

  sendMessage(
    sessionId: string,
    payload: SendMessageRequest,
    idempotencyKey?: string,
  ): Promise<ChatMessageResponse> {
    const headers: Record<string, string> = {};
    if (idempotencyKey) {
      headers['Idempotency-Key'] = idempotencyKey;
    }
    return this.client.request<ChatMessageResponse>(
      `/chat/sessions/${sessionId}/messages`,
      { method: 'POST', body: payload, headers },
    );
  }
}
