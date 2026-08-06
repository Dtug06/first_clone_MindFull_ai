/**
 * Daily Question (Daily Check-in) API — wrappers for /daily-checkins/* endpoints.
 *
 * DTOs mirror docs/03_API_CONTRACT.yaml entries:
 *   DailyQuestionAssignmentResponse, DailyQuestionOptionResponse,
 *   DailyAnswerRequest, DailyAnswerResponse, CheckinHistoryResponse.
 *
 * Pages render one card per assignment, dispatching on `questionType`:
 *   SCALE          → NUMERIC answer (1..10)
 *   SINGLE_CHOICE  → OPTION answer (value from options[])
 *   TEXT           → TEXT  answer (free text, ≤ 5000 chars per G2-T06 cap)
 *   NUMBER         → NUMERIC answer (any numeric)
 */

import { ApiClient } from './client';

export type DailyQuestionType = 'SCALE' | 'SINGLE_CHOICE' | 'TEXT' | 'NUMBER';
export type DailyAnswerType = 'NUMERIC' | 'TEXT' | 'OPTION';

export interface DailyQuestionOptionResponse {
  value: string;
  label: string;
  orderIndex: number;
}

export interface DailyQuestionAssignmentResponse {
  assignmentId: string;
  templateCode: string;
  questionType: DailyQuestionType;
  prompt: string;
  assignedForDate: string;
  options?: DailyQuestionOptionResponse[];
  answered: boolean;
}

export interface DailyAnswerRequest {
  answerType: DailyAnswerType;
  numericValue?: number;
  textValue?: string;
  optionValue?: string;
}

export interface DailyAnswerResponse {
  id: string;
  assignmentId: string;
  answeredAt: string;
}

export interface CheckinHistoryResponse {
  date: string;
  timezone: string;
  answers: DailyAnswerResponse[];
}

/** Approved backend template code for the optional free-text daily note. */
export const OPEN_NOTE_TEMPLATE_CODE = 'OPEN';

export class DailyQuestionApi {
  constructor(private readonly client: ApiClient) {}

  today(): Promise<DailyQuestionAssignmentResponse[]> {
    return this.client.request<DailyQuestionAssignmentResponse[]>('/daily-checkins/today', {
      method: 'GET',
    });
  }

  history(): Promise<CheckinHistoryResponse[]> {
    return this.client.request<CheckinHistoryResponse[]>('/daily-checkins/history', {
      method: 'GET',
    });
  }

  submitAnswer(
    assignmentId: string,
    payload: DailyAnswerRequest,
    idempotencyKey?: string,
  ): Promise<DailyAnswerResponse> {
    const headers: Record<string, string> = {};
    if (idempotencyKey) {
      headers['Idempotency-Key'] = idempotencyKey;
    }
    return this.client.request<DailyAnswerResponse>(
      `/daily-checkins/${assignmentId}/answer`,
      { method: 'POST', body: payload, headers },
    );
  }
}
