/**
 * Consent API — minimal wrappers for the consent endpoints introduced in
 * G1-T08. Used by the Auth page to demonstrate that the frontend can talk
 * to a real backend.
 */

import { ApiClient } from './client';

export type ConsentType = 'CHAT_ANALYSIS' | 'PERSONALIZATION' | 'EXPERT_SHARING';
export type ConsentAction = 'GRANTED' | 'REVOKED';

export interface CurrentConsentResponse {
  consents: Array<{
    consentType: ConsentType;
    state: ConsentAction;
    policyVersion: string;
    updatedAt: string;
  }>;
}

export interface ConsentRecordRequest {
  consentType: ConsentType;
  action: ConsentAction;
  policyVersion: string;
}

export class ConsentsApi {
  constructor(private readonly client: ApiClient) {}

  current(): Promise<CurrentConsentResponse> {
    return this.client.request<CurrentConsentResponse>('/consents/current', { method: 'GET' });
  }

  record(payload: ConsentRecordRequest): Promise<unknown> {
    return this.client.request('/consents', { method: 'POST', body: payload });
  }
}
