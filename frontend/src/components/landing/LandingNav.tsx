import { motion } from 'framer-motion';
import { Link, useLocation } from 'react-router-dom';
import { useState } from 'react';
import { Menu, X } from 'lucide-react';
import { useLanguage } from '../../i18n';
import LanguageSwitcher from '../ui/LanguageSwitcher';

export default function LandingNav() {
  const [isOpen, setIsOpen] = useState(false);
  const location = useLocation();
  const { t } = useLanguage();

  const handleLogoClick = () => {
    if (location.pathname === '/') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const navLinks = [
    { sectionId: 'features', label: t.nav.features },
    { sectionId: 'how-it-works', label: t.nav.howItWorks },
    { sectionId: 'safety', label: t.nav.safety },
  ];

  const scrollToSection = (sectionId: string) => {
    document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth' });
    setIsOpen(false);
  };

  return (
    <nav className="fixed top-0 left-0 right-0 z-50 bg-background/80 backdrop-blur-lg border-b border-gray-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" onClick={handleLogoClick} className="flex items-center gap-2">
            <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-primary to-primaryDark flex items-center justify-center">
              <svg className="w-5 h-5 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2C8 2 5 5 5 9c0 3 2 5 3 6v3c0 1 1 2 2 2h4c1 0 2-1 2-2v-3c1-1 3-3 3-6 0-4-3-7-7-7z"/>
              </svg>
            </div>
            <span className="text-lg font-semibold text-textMain">{t.common.appName}</span>
          </Link>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center gap-8">
            {navLinks.map((link) => (
              <button
                type="button"
                key={link.sectionId}
                onClick={() => scrollToSection(link.sectionId)}
                className="text-sm font-medium transition-colors text-textMuted hover:text-textMain"
              >
                {link.label}
              </button>
            ))}
            <Link to="/app" className="text-sm font-medium transition-colors text-textMuted hover:text-textMain">
              {t.nav.tryApp}
            </Link>
          </div>

          {/* CTA */}
          <div className="hidden md:flex items-center gap-4">
            <LanguageSwitcher variant="pill" />
            <Link to="/app" className="btn-primary text-sm">
              {t.nav.startFree}
            </Link>
          </div>

          {/* Mobile language + menu */}
          <div className="md:hidden flex items-center gap-1">
            <LanguageSwitcher variant="compact" />
            <button
              className="p-2 rounded-lg hover:bg-gray-100"
              onClick={() => setIsOpen(!isOpen)}
            >
              {isOpen ? (
                <X className="w-5 h-5 text-textMain" />
              ) : (
                <Menu className="w-5 h-5 text-textMain" />
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      <motion.div
        className={`md:hidden ${isOpen ? 'block' : 'hidden'}`}
        initial={false}
        animate={{ height: isOpen ? 'auto' : 0, opacity: isOpen ? 1 : 0 }}
        transition={{ duration: 0.2 }}
      >
        <div className="bg-surface border-t border-gray-100 px-4 py-4 space-y-2">
          {navLinks.map((link) => (
            <button
              type="button"
              key={link.sectionId}
              className="block w-full px-4 py-3 text-left text-textMuted hover:text-textMain hover:bg-gray-50 rounded-xl"
              onClick={() => scrollToSection(link.sectionId)}
            >
              {link.label}
            </button>
          ))}
          <Link
            to="/app"
            className="block px-4 py-3 text-textMuted hover:text-textMain hover:bg-gray-50 rounded-xl"
            onClick={() => setIsOpen(false)}
          >
            {t.nav.tryApp}
          </Link>
          <Link
            to="/app"
            className="block px-4 py-3 bg-primary text-white text-center rounded-xl font-medium"
            onClick={() => setIsOpen(false)}
          >
            {t.nav.startFree}
          </Link>
        </div>
      </motion.div>
    </nav>
  );
}
