import { useState } from 'react';
import { Outlet, useNavigate, Navigate } from 'react-router-dom';
import MobileNavigation from '../../components/layout/MobileNavigation';
import PageTransitionWrapper from '../../components/layout/PageTransitionWrapper';
import JellyfishMascot from '../../components/ui/JellyfishMascot';
import { useAuth } from '../../auth/AuthContext';

export default function UserLayout() {
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const { token } = useAuth();
  const navigate = useNavigate();

  if (!token) {
    // Frontend guard mirrors backend's 401 contract — no token means no
    // authenticated principal, so pages would only fail every request.
    // Backend stays the source of truth for ownership/authz.
    return <Navigate to="/auth" replace />;
  }

  return (
    <div className="min-h-screen bg-background overflow-x-hidden w-full relative">
      {/* Main content */}
      <div className="pb-20 lg:pb-0 w-full">
        <PageTransitionWrapper>
          <Outlet />
        </PageTransitionWrapper>
      </div>

      {/* Mobile Navigation */}
      <MobileNavigation
        isOpen={mobileNavOpen}
        onClose={() => setMobileNavOpen(false)}
      />

      {/* Desktop Navigation - only shows on lg+ */}
      <nav className="hidden lg:flex fixed bottom-8 inset-x-0 z-40 justify-center pointer-events-none px-4">
        <div className="bg-surface/90 backdrop-blur-lg rounded-full px-4 py-3 shadow-soft-lg border border-gray-100 pointer-events-auto">
          <div className="flex items-center gap-1">
            {[
              { path: '/app', label: 'Home', icon: '🏠' },
              { path: '/app/check-in', label: 'Check-in', icon: '💚' },
              { path: '/app/chat', label: 'Chat', icon: '💬' },
              { path: '/app/dashboard', label: 'Dashboard', icon: '📊' },
              { path: '/app/library', label: 'Library', icon: '📚' },
            ].map((item) => (
              <a
                key={item.path}
                href={item.path}
                className="px-3 py-2 rounded-full text-sm font-medium text-textMuted hover:text-primary hover:bg-primary/5 transition-all whitespace-nowrap"
              >
                <span className="mr-1">{item.icon}</span>
                {item.label}
              </a>
            ))}
            {/* Sign out shortcut (visible only on authenticated layout) */}
            <button
              type="button"
              onClick={() => {
                localStorage.removeItem('mb:auth');
                navigate('/auth', { replace: true });
                window.location.reload();
              }}
              className="px-3 py-2 rounded-full text-sm font-medium text-textMuted hover:text-primary hover:bg-primary/5 transition-all whitespace-nowrap"
              aria-label="Sign out"
            >
              <span className="mr-1">🚪</span>
              Sign out
            </button>
          </div>
        </div>
      </nav>

      {/* Floating jellyfish mascot - desktop only, bottom-left to avoid overlap with center nav */}
      <div className="hidden lg:block fixed bottom-8 left-6 z-30">
        <div className="relative flex flex-col items-center">
          <JellyfishMascot size="md" animated />
          <div className="mt-1 px-3 py-1 bg-primary/10 rounded-full text-xs text-primary font-medium whitespace-nowrap">
            Hi there!
          </div>
        </div>
      </div>
    </div>
  );
}