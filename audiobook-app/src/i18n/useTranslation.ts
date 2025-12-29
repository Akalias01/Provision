import { useStore } from '../store/useStore';
import { translations, type TranslationKey, type Language } from './translations';

export function useTranslation() {
  const language = useStore((state) => state.language) as Language;

  const t = (key: TranslationKey): string => {
    const lang = translations[language] || translations.en;
    return lang[key] || translations.en[key] || key;
  };

  return { t, language };
}

export { type TranslationKey, type Language };
