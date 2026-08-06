import { useState } from 'react';
import { motion } from 'framer-motion';
import { selfHelpArticles } from '../../data';
import { useLanguage } from '../../i18n';
import { Wind, Heart, Moon, BookOpen, Brain, Scale, MessageCircle, Edit3, Search } from 'lucide-react';
import { LucideIcon } from 'lucide-react';

const iconMap: Record<string, LucideIcon> = {
  Wind,
  Heart,
  Moon,
  BookOpen,
  Brain,
  Scale,
  MessageCircle,
  Edit3,
};

const categoryKeys = [
  'breathing',
  'stressManagement',
  'sleepHygiene',
  'journaling',
  'cbt',
  'dbt',
  'communication',
] as const;

export default function SelfHelpLibrary() {
  const { t } = useLanguage();
  const [selectedCategoryKey, setSelectedCategoryKey] = useState<'all' | typeof categoryKeys[number]>('all');
  const [searchQuery, setSearchQuery] = useState('');

  const allLabel = t.data.articleCategories.all;
  const categoryMap = t.data.articleCategories;

  const categories = [
    { key: 'all' as const, label: allLabel, count: selfHelpArticles.length },
    ...categoryKeys.map((key) => ({
      key,
      label: categoryMap[key],
      count: selfHelpArticles.filter((a) => a.categoryKey === key).length,
    })),
  ];

  const getCategoryLabel = (key: typeof categoryKeys[number]) => categoryMap[key];

  const filteredArticles = selfHelpArticles.filter((article) => {
    const matchesCategory = selectedCategoryKey === 'all' || article.categoryKey === selectedCategoryKey;
    const articleT = t.data.articles[article.i18nKey];
    const title = articleT.title;
    const content = articleT.content;
    const matchesSearch =
      title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      content.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  return (
    <div className="min-h-screen bg-background pb-24 lg:pb-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Header */}
        <motion.div
          className="mb-8"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <h1 className="text-2xl font-semibold text-textMain mb-1">
            {t.user.libraryTitle}
          </h1>
          <p className="text-textMuted">{t.user.librarySubtitle}</p>
        </motion.div>

        {/* Search */}
        <motion.div
          className="mb-6"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <div className="relative">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-textMuted" />
            <input
              type="text"
              placeholder={t.data.ui.searchExercises}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-12 pr-4 py-3 bg-surface rounded-2xl text-textMain placeholder:text-textMuted/60 focus:outline-none focus:ring-2 focus:ring-primary/20 border border-transparent focus:border-primary/20"
            />
          </div>
        </motion.div>

        {/* Categories */}
        <motion.div
          className="mb-6 overflow-x-auto"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
        >
          <div className="flex gap-2 pb-2">
            {categories.map((cat) => (
              <button
                key={cat.key}
                onClick={() => setSelectedCategoryKey(cat.key)}
                className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all ${
                  selectedCategoryKey === cat.key
                    ? 'bg-primary text-white shadow-glow-sm'
                    : 'bg-surface text-textMuted hover:bg-surfaceMuted'
                }`}
              >
                {cat.label}
                <span className="ml-1 text-xs opacity-60">({cat.count})</span>
              </button>
            ))}
          </div>
        </motion.div>

        {/* Articles */}
        <div className="space-y-4">
          {filteredArticles.map((article, index) => {
            const Icon = iconMap[article.icon] || BookOpen;
            const articleT = t.data.articles[article.i18nKey];
            return (
              <motion.div
                key={article.id}
                className="bg-surface rounded-2xl p-5 shadow-soft border border-gray-100 cursor-pointer hover:shadow-soft-lg transition-all"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 + index * 0.05 }}
                whileHover={{ y: -2 }}
              >
                <div className="flex items-start gap-4">
                  <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
                    <Icon className="w-6 h-6 text-primary" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-medium text-textMain">{articleT.title}</h3>
                      <span className="px-2 py-0.5 bg-surfaceMuted rounded-full text-xs text-textMuted">
                        {getCategoryLabel(article.categoryKey)}
                      </span>
                    </div>
                    <p className="text-sm text-textMuted mb-2">{articleT.content}</p>
                    <div className="flex items-center gap-2 text-xs text-textMuted">
                      <span className="flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-primary" />
                        {article.duration}
                      </span>
                    </div>
                  </div>
                </div>
              </motion.div>
            );
          })}
        </div>

        {filteredArticles.length === 0 && (
          <motion.div
            className="text-center py-12"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
          >
            <p className="text-textMuted">{t.data.ui.noArticlesFound}</p>
          </motion.div>
        )}
      </div>
    </div>
  );
}
