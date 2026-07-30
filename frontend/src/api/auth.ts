/**
 * Authentication API — DTOs and call wrappers.
 *
 * DTOs mirror entries in docs/03_API_CONTRACT.yaml
 * (RegisterRequest, LoginRequest, AuthResponse, UserResponse).
 */

import { ApiClient } from './client';

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UserResponse {
  id: string;
  email: string;
  displayName: string;
  role: string;
  status: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
  user: UserResponse;
}

export class AuthApi {
  constructor(private readonly client: ApiClient) {}

  register(payload: RegisterRequest): Promise<AuthResponse> {
    return this.client.request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: payload,
    });
  }

  login(payload: LoginRequest): Promise<AuthResponse> {
    return this.client.request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: payload,
    });
  }

  me(): Promise<UserResponse> {
    return this.client.request<UserResponse>('/users/me', { method: 'GET' });
  }
}
