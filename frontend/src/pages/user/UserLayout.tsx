import { useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate, Navigate } from 'react-router-dom';
import MobileNavigation from '../../components/layout/MobileNavigation';
import PageTransitionWrapper from '../../components/layout/PageTransitionWrapper';
import LanguageSwitcher from '../../components/ui/LanguageSwitcher';
import { useAuth } from '../../auth/AuthContext';
import { useLanguage } from '../../i18n';
import { clearChatSessionId } from '../../lib/accountStorage';

export default function UserLayout() {
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const { token, user, logout } = useAuth();
  const { t } = useLanguage();
  const location = useLocation();
  const navigate = useNavigate();

  if (!token) {
    return <Navigate to="/auth" replace />;
  }

  const isOnboarding = location.pathname === '/app/onboarding';
  const isChatRoute = location.pathname.startsWith('/app/chat');
  const showNavigation = !isOnboarding;

  const navItems = [
    { path: '/app', label: t.nav.home, icon: '🏠', end: true },
    { path: '/app/daily', label: t.nav.checkIn, icon: '💚' },
    { path: '/app/chat', label: t.nav.aiChat, icon: '💬' },
    { path: '/app/dashboard', label: t.nav.dashboard, icon: '📊' },
    { path: '/app/library', label: t.nav.library, icon: '📚' },
    { path: '/app/settings', label: t.nav.settings, icon: '⚙️' },
  ];

  const signOut = () => {
    clearChatSessionId(user?.id ?? null);
    logout();
    navigate('/auth', { replace: true });
  };

  return (
    <div className={`${isChatRoute ? 'h-screen overflow-hidden' : 'min-h-screen'} bg-background overflow-x-hidden w-full relative`}>
      <div className="absolute top-4 right-4 z-50">
        <LanguageSwitcher variant="pill" />
      </div>

      <div className={`w-full ${isChatRoute ? 'h-full overflow-hidden' : `${showNavigation ? 'pb-20 lg:pb-8 lg:pl-64' : 'pb-8'}`}`}>
        <PageTransitionWrapper fullHeight={isChatRoute}>
          <Outlet />
        </PageTransitionWrapper>
      </div>

      {showNavigation && (
        <MobileNavigation isOpen={mobileNavOpen} onClose={() => setMobileNavOpen(false)} />
      )}

      {showNavigation && (
        <aside className="hidden lg:flex fixed top-0 left-0 bottom-0 z-40 w-64 flex-col bg-surface/95 backdrop-blur-lg border-r border-gray-100">
          <div className="px-6 py-6 border-b border-gray-100">
            <NavLink to="/app" className="flex items-center gap-2">
              <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-primary to-primaryDark flex items-center justify-center">
                <svg className="w-5 h-5 text-white" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C8 2 5 5 5 9c0 3 2 5 3 6v3c0 1 1 2 2 2h4c1 0 2-1 2-2v-3c1-1 3-3 3-6 0-4-3-7-7-7z"/>
                </svg>
              </div>
              <span className="text-lg font-semibold text-textMain">{t.common.appName}</span>
            </NavLink>
          </div>

          <nav className="flex-1 px-4 py-6 space-y-1 overflow-y-auto">
            {navItems.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                end={item.end}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-4 py-3 rounded-2xl text-sm font-medium transition-all ${
                    isActive
                      ? 'bg-primary/10 text-primary'
                      : 'text-textMuted hover:text-textMain hover:bg-gray-50'
                  }`
                }
              >
                <span className="text-lg">{item.icon}</span>
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="px-4 pb-3">
            <NavLink
              to="/app/emergency"
              className={({ isActive }) =>
                `flex items-center gap-3 px-4 py-3 rounded-2xl text-sm font-semibold border transition-all ${
                  isActive
                    ? 'bg-red-500 text-white border-red-500'
                    : 'bg-red-50 text-red-600 border-red-100 hover:bg-red-100'
                }`
              }
            >
              <span className="text-lg">🚨</span>
              {t.nav.emergency}
            </NavLink>
          </div>

          <div className="px-4 py-4 border-t border-gray-100">
            <button
              type="button"
              onClick={signOut}
              className="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-sm font-medium text-textMuted hover:text-red-600 hover:bg-red-50 transition-all"
            >
              <span className="text-lg">🚪</span>
              {t.common.signOut}
            </button>
          </div>
        </aside>
      )}
    </div>
  );
}
