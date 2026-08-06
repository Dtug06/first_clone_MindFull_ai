import { useState, useRef, useEffect } from 'react';
import { useLanguage, languages, Language } from '../../i18n';

type Variant = 'pill' | 'compact';

interface LanguageSwitcherProps {
  /** 'pill' = rounded-full with flag + name + chevron (used in topbar / sidebar).
   *  'compact' = flag only, square (used in dense nav bars). */
  variant?: Variant;
  className?: string;
}
// Tiny inline flag SVGs – keeps the bundle small and removes image requests.
const FlagVI = ({ className }: { className?: string }) => (
  <svg
    className={className}
    viewBox="0 0 24 16"
    aria-hidden="true"
    shapeRendering="geometricPrecision"
  >
    <rect width="24" height="16" rx="2" fill="#DA251D" />
    <path
      d="M12 3.2 L13.1 6.6 L16.6 6.6 L13.7 8.6 L14.8 12 L12 10 L9.2 12 L10.3 8.6 L7.4 6.6 L10.9 6.6 Z"
      fill="#FFFF00"
    />
  </svg>
);

const FlagEN = ({ className }: { className?: string }) => (
  <svg
    className={className}
    viewBox="0 0 24 16"
    aria-hidden="true"
    shapeRendering="geometricPrecision"
  >
    <clipPath id="en-clip">
      <rect width="24" height="16" rx="2" />
    </clipPath>
    <g clipPath="url(#en-clip)">
      <rect width="24" height="16" fill="#012169" />
      <path d="M0 0 L24 16 M24 0 L0 16" stroke="#FFFFFF" strokeWidth="2.4" />
      <path
        d="M0 0 L24 16 M24 0 L0 16"
        stroke="#C8102E"
        strokeWidth="1.2"
        clipPath="url(#en-clip-thin)"
      />
      <path d="M12 0 V16 M0 8 H24" stroke="#FFFFFF" strokeWidth="4" />
      <path d="M12 0 V16 M0 8 H24" stroke="#C8102E" strokeWidth="2.4" />
    </g>
  </svg>
);

const flagByCode: Record<Language, (props: { className?: string }) => JSX.Element> = {
  vi: FlagVI,
  en: FlagEN,
};

export default function LanguageSwitcher({
  variant = 'pill',
  className = '',
}: LanguageSwitcherProps) {
  const { language, setLanguage } = useLanguage();
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!open) return;
    const onClick = (e: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onClick);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const current = languages.find((l) => l.code === language) ?? languages[0];
  const CurrentFlag = flagByCode[current.code];

  const select = (code: Language) => {
    setLanguage(code);
    setOpen(false);
  };

  if (variant === 'compact') {
    return (
      <div ref={wrapperRef} className={`relative ${className}`}>
        <button
          type="button"
          onClick={() => setOpen((v) => !v)}
          aria-haspopup="listbox"
          aria-expanded={open}
          aria-label="Select language"
          className="flex items-center gap-1.5 px-2 py-1.5 rounded-lg text-textMuted hover:text-textMain hover:bg-gray-100/80 transition-colors"
        >
          <CurrentFlag className="w-5 h-3.5 rounded-sm shadow-sm" />
        </button>
        {open && (
          <LanguageDropdown
            current={language}
            onSelect={select}
            align="right"
          />
        )}
      </div>
    );
  }

  // pill variant
  return (
    <div ref={wrapperRef} className={`relative ${className}`}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-haspopup="listbox"
        aria-expanded={open}
        className={`group flex items-center gap-2 pl-2 pr-2.5 py-1.5 rounded-full border transition-all
          ${open
            ? 'border-primary/40 bg-primary/5 shadow-sm'
            : 'border-gray-200 bg-white hover:border-gray-300 hover:shadow-sm'
          }`}
      >
        <CurrentFlag className="w-5 h-3.5 rounded-sm shadow-sm flex-shrink-0" />
        <span className="text-xs font-semibold uppercase tracking-wide text-textMain">
          {current.code}
        </span>
        <svg
          className={`w-3.5 h-3.5 text-textMuted transition-transform ${open ? 'rotate-180' : ''}`}
          viewBox="0 0 20 20"
          fill="currentColor"
        >
          <path
            fillRule="evenodd"
            d="M5.23 7.21a.75.75 0 011.06.02L10 11.06l3.71-3.83a.75.75 0 111.08 1.04l-4.25 4.39a.75.75 0 01-1.08 0L5.21 8.27a.75.75 0 01.02-1.06z"
            clipRule="evenodd"
          />
        </svg>
      </button>
      {open && (
        <LanguageDropdown
          current={language}
          onSelect={select}
          align="right"
        />
      )}
    </div>
  );
}

interface DropdownProps {
  current: Language;
  onSelect: (code: Language) => void;
  align: 'right' | 'left';
}

function LanguageDropdown({ current, onSelect, align }: DropdownProps) {
  return (
    <div
      role="listbox"
      className={`absolute z-50 mt-2 min-w-[180px] rounded-2xl border border-gray-100 bg-white/95 backdrop-blur-lg shadow-soft-lg overflow-hidden
        ${align === 'right' ? 'right-0' : 'left-0'}`}
    >
      <div className="px-3 py-2 text-[11px] font-medium uppercase tracking-wider text-textMuted/80 border-b border-gray-100">
        Language / Ngôn ngữ
      </div>
      {languages.map((lang) => {
        const Flag = flagByCode[lang.code];
        const isActive = lang.code === current;
        return (
          <button
            key={lang.code}
            type="button"
            role="option"
            aria-selected={isActive}
            onClick={() => onSelect(lang.code)}
            className={`w-full flex items-center gap-3 px-3 py-2.5 text-sm transition-colors
              ${isActive
                ? 'bg-primary/5 text-primary'
                : 'text-textMain hover:bg-gray-50'
              }`}
          >
            <Flag className="w-6 h-4 rounded shadow-sm flex-shrink-0" />
            <div className="flex-1 text-left">
              <div className="font-medium leading-tight">{lang.nativeLabel}</div>
              <div className="text-[11px] text-textMuted leading-tight">{lang.label}</div>
            </div>
            {isActive && (
              <svg
                className="w-4 h-4 text-primary flex-shrink-0"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fillRule="evenodd"
                  d="M16.7 5.3a1 1 0 010 1.4l-7.5 7.5a1 1 0 01-1.4 0L3.3 9.7a1 1 0 011.4-1.4L8.5 12 15.3 5.3a1 1 0 011.4 0z"
                  clipRule="evenodd"
                />
              </svg>
            )}
          </button>
        );
      })}
    </div>
  );
}
