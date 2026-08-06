export type Language = 'en' | 'vi';

export const languages: { code: Language; label: string; nativeLabel: string }[] = [
  { code: 'vi', label: 'Vietnamese', nativeLabel: 'Tiếng Việt' },
  { code: 'en', label: 'English', nativeLabel: 'English' },
];

export const defaultLanguage: Language = 'vi';

export const getStoredLanguage = (): Language => {
  const stored = localStorage.getItem('mindbridge-language');
  if (stored === 'en' || stored === 'vi') {
    return stored;
  }
  return defaultLanguage;
};
export const setStoredLanguage = (lang: Language): void => {
  localStorage.setItem('mindbridge-language', lang);
};
