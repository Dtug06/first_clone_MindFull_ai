import { Link, Routes, Route } from 'react-router-dom';
import { Suspense } from 'react';
import LandingNav from './components/landing/LandingNav';
import HeroSection from './components/landing/HeroSection';
import ProblemSection from './components/landing/ProblemSection';
import HowItWorksSection from './components/landing/HowItWorksSection';
import FeaturesSection from './components/landing/FeaturesSection';
import JellyfishCompanionSection from './components/landing/JellyfishCompanionSection';
import SafetyPrivacySection from './components/landing/SafetyPrivacySection';
import OrganizationsSection from './components/landing/OrganizationsSection';
import FinalCTASection from './components/landing/FinalCTASection';

// User pages
import UserLayout from './pages/user/UserLayout';
import UserHome from './pages/user/UserHome';
import MoodCheckIn from './pages/user/MoodCheckIn';
import AIChat from './pages/user/AIChat';
import Dashboard from './pages/user/Dashboard';
import SelfHelpLibrary from './pages/user/SelfHelpLibrary';
import EmergencySupport from './pages/user/EmergencySupport';

// G1-T10: minimal auth page backed by the real backend API
import AuthPage from './pages/AuthPage';

// Admin pages
import AdminLayout from './pages/admin/AdminLayout';
import AdminOverview from './pages/admin/AdminOverview';
import UserManagement from './pages/admin/UserManagement';
import RiskMonitoring from './pages/admin/RiskMonitoring';
import ExpertManagement from './pages/admin/ExpertManagement';
import ContentLibrary from './pages/admin/ContentLibrary';
import AIKnowledgeBase from './pages/admin/AIKnowledgeBase';
import OrganizationDashboard from './pages/admin/OrganizationDashboard';
import AdminSettings from './pages/admin/AdminSettings';

// Expert pages
import ExpertLayout from './pages/expert/ExpertLayout';
import ExpertCases from './pages/expert/ExpertCases';
import ExpertCaseDetail from './pages/expert/ExpertCaseDetail';

// Loading component
function LoadingSpinner() {
  return (
    <div className="min-h-screen bg-background flex items-center justify-center">
      <div className="w-12 h-12 rounded-full border-4 border-primary/20 border-t-primary animate-spin" />
    </div>
  );
}

// Landing Page
function LandingPage() {
  return (
    <>
      <LandingNav />
      <main className="pt-16">
        <HeroSection />
        <ProblemSection />
        <HowItWorksSection />
        <FeaturesSection />
        <JellyfishCompanionSection />
        <SafetyPrivacySection />
        <OrganizationsSection />
        <FinalCTASection />
        
        {/* Footer */}
        <footer className="bg-oceanDeep text-white py-12">
          <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="grid md:grid-cols-4 gap-8">
              <div>
                <div className="flex items-center gap-2 mb-4">
                  <div className="w-9 h-9 rounded-lg bg-white/10 flex items-center justify-center">
                    <svg className="w-5 h-5 text-white" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 2C8 2 5 5 5 9c0 3 2 5 3 6v3c0 1 1 2 2 2h4c1 0 2-1 2-2v-3c1-1 3-3 3-6 0-4-3-7-7-7z"/>
                    </svg>
                  </div>
                  <span className="font-semibold">MindBridge AI</span>
                </div>
                <p className="text-sm text-white/70">
                  A calmer bridge to your inner world. AI-powered mental health support.
                </p>
              </div>
              <div>
                <h4 className="font-medium mb-4">Product</h4>
                <ul className="space-y-2 text-sm text-white/70">
                  <li><a href="#features" className="hover:text-white transition-colors">Features</a></li>
                  <li><a href="#how-it-works" className="hover:text-white transition-colors">How it works</a></li>
                  <li><Link to="/app" className="hover:text-white transition-colors">Try app</Link></li>
                </ul>
              </div>
              <div>
                <h4 className="font-medium mb-4">Resources</h4>
                <ul className="space-y-2 text-sm text-white/70">
                  <li><a href="#safety" className="hover:text-white transition-colors">Safety</a></li>
                  <li><a href="#" className="hover:text-white transition-colors">Privacy</a></li>
                  <li><a href="#" className="hover:text-white transition-colors">Terms</a></li>
                </ul>
              </div>
              <div>
                <h4 className="font-medium mb-4">Contact</h4>
                <ul className="space-y-2 text-sm text-white/70">
                  <li>support@mindbridge.ai</li>
                  <li>For organizations</li>
                </ul>
              </div>
            </div>
            <div className="mt-8 pt-8 border-t border-white/10 text-center text-sm text-white/50">
              MindBridge AI does not replace professional psychological support.
            </div>
          </div>
        </footer>
      </main>
    </>
  );
}

export default function App() {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <Routes>
        {/* Landing Page */}
        <Route path="/" element={<LandingPage />} />

        {/* G1-T10: Auth page (sign in / register) — not protected */}
        <Route path="/auth" element={<AuthPage />} />

        {/* User App */}
        <Route path="/app" element={<UserLayout />}>
          <Route index element={<UserHome />} />
          <Route path="check-in" element={<MoodCheckIn />} />
          <Route path="chat" element={<AIChat />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="library" element={<SelfHelpLibrary />} />
          <Route path="emergency" element={<EmergencySupport />} />
        </Route>

        {/* Admin Dashboard */}
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<AdminOverview />} />
          <Route path="users" element={<UserManagement />} />
          <Route path="risk" element={<RiskMonitoring />} />
          <Route path="experts" element={<ExpertManagement />} />
          <Route path="content" element={<ContentLibrary />} />
          <Route path="ai" element={<AIKnowledgeBase />} />
          <Route path="organizations" element={<OrganizationDashboard />} />
          <Route path="settings" element={<AdminSettings />} />
        </Route>

        {/* Expert Portal */}
        <Route path="/expert" element={<ExpertLayout />}>
          <Route index element={<ExpertCases />} />
          <Route path="cases" element={<ExpertCases />} />
          <Route path="cases/:eventId" element={<ExpertCaseDetail />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
