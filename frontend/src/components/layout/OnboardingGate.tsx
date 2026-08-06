import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useUser } from '../../contexts/UserContext';

export default function OnboardingGate() {
  const { hasCompletedOnboarding } = useUser();
  const location = useLocation();

  if (!hasCompletedOnboarding) {
    return <Navigate to="/app/onboarding" replace state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}
