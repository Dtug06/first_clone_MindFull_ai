import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

type Mode = 'login' | 'register';

export default function AuthPage() {
  const navigate = useNavigate();
  const { login, register, loading, user, lastRequestId, primeLastRequestId, logout } = useAuth();

  const [mode, setMode] = useState<Mode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Array<{ field: string; message: string }>>([]);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setFieldErrors([]);
    try {
      if (mode === 'login') {
        await login({ email: email.trim(), password });
      } else {
        await register({ email: email.trim(), password, displayName: displayName.trim() });
      }
      primeLastRequestId(null);
      navigate('/app', { replace: true });
    } catch (e) {
      if (e instanceof ApiError) {
        setError(e.message);
        setFieldErrors(e.fieldErrors ? [...e.fieldErrors] : []);
        primeLastRequestId(e.requestId);
      } else if (e instanceof Error) {
        setError(e.message || 'Unexpected error');
      } else {
        setError('Unexpected error');
      }
    }
  };

  return (
    <main className="min-h-screen bg-background flex flex-col">
      <header className="px-6 py-4 border-b border-gray-100">
        <Link to="/" className="text-sm text-textMuted hover:text-textMain">
          ← Back to landing
        </Link>
      </header>

      <section className="flex-1 flex items-center justify-center px-4">
        <div className="w-full max-w-md bg-surface rounded-2xl shadow-sm border border-gray-100 p-8">
          <h1 className="text-2xl font-semibold text-textMain mb-2">
            {mode === 'login' ? 'Sign in to MindBridge' : 'Create your MindBridge account'}
          </h1>
          <p className="text-sm text-textMuted mb-6">
            Backed by the real API. Mock data is no longer used on the auth screen.
          </p>

          <div className="flex gap-2 mb-6">
            <button
              type="button"
              className={`flex-1 py-2 rounded-xl text-sm font-medium transition-colors ${
                mode === 'login'
                  ? 'bg-primary text-white'
                  : 'bg-surfaceMuted text-textMuted hover:text-textMain'
              }`}
              onClick={() => setMode('login')}
            >
              Sign in
            </button>
            <button
              type="button"
              className={`flex-1 py-2 rounded-xl text-sm font-medium transition-colors ${
                mode === 'register'
                  ? 'bg-primary text-white'
                  : 'bg-surfaceMuted text-textMuted hover:text-textMain'
              }`}
              onClick={() => setMode('register')}
            >
              Register
            </button>
          </div>

          {user ? (
            <div className="mb-4 p-3 rounded-xl bg-surfaceMuted text-sm">
              <p className="text-textMain">
                Signed in as <strong>{user.email}</strong> ({user.role}).
              </p>
              <button
                type="button"
                onClick={logout}
                className="mt-2 text-xs text-primary hover:underline"
              >
                Sign out
              </button>
            </div>
          ) : null}

          <form onSubmit={submit} className="space-y-4">
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-textMain mb-1">
                Email
              </label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full px-4 py-2 rounded-xl bg-surfaceMuted text-textMain border border-transparent focus:border-primary/30 focus:outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-textMain mb-1">
                Password
              </label>
              <input
                id="password"
                type="password"
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                required
                minLength={8}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full px-4 py-2 rounded-xl bg-surfaceMuted text-textMain border border-transparent focus:border-primary/30 focus:outline-none focus:ring-2 focus:ring-primary/20"
              />
              {mode === 'register' && (
                <p className="mt-1 text-xs text-textMuted">Minimum 8 characters.</p>
              )}
            </div>

            {mode === 'register' && (
              <div>
                <label htmlFor="displayName" className="block text-sm font-medium text-textMain mb-1">
                  Display name
                </label>
                <input
                  id="displayName"
                  type="text"
                  required
                  minLength={1}
                  maxLength={100}
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  className="w-full px-4 py-2 rounded-xl bg-surfaceMuted text-textMain border border-transparent focus:border-primary/30 focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
            )}

            {error && (
              <div role="alert" className="text-sm text-red-600 bg-red-50 px-3 py-2 rounded-xl">
                {error}
                {fieldErrors.length > 0 && (
                  <ul className="mt-1 list-disc list-inside text-xs">
                    {fieldErrors.map((fe) => (
                      <li key={fe.field}>{fe.field}: {fe.message}</li>
                    ))}
                  </ul>
                )}
              </div>
            )}

            {lastRequestId && (
              <p className="text-xs text-textMuted">
                Trace ID:{' '}
                <span className="font-mono" data-testid="last-request-id">{lastRequestId}</span>
              </p>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full btn-primary text-sm disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {loading ? 'Working…' : mode === 'login' ? 'Sign in' : 'Create account'}
            </button>
          </form>
        </div>
      </section>
    </main>
  );
}
