import { motion } from 'framer-motion';
import { selfHelpArticles } from '../../data';
import { Plus, Edit3, Trash2, CheckCircle } from 'lucide-react';

export default function ContentLibrary() {
  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        className="flex flex-col sm:flex-row sm:items-center justify-between gap-4"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <div>
          <h1 className="text-2xl font-semibold text-textMain">Content Library</h1>
          <p className="text-textMuted">Manage self-help articles and resources.</p>
        </div>
        <motion.button
          className="inline-flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-xl text-sm font-medium"
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
        >
          <Plus className="w-4 h-4" />
          Add Content
        </motion.button>
      </motion.div>

      {/* Content list */}
      <div className="space-y-4">
        {selfHelpArticles.map((article, index) => (
          <motion.div
            key={article.id}
            className="bg-surface rounded-2xl p-5 shadow-soft border border-gray-100"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 + index * 0.05 }}
          >
            <div className="flex items-center justify-between">
              <div className="flex-1">
                <div className="flex items-center gap-3 mb-2">
                  <h3 className="font-semibold text-textMain">{article.title}</h3>
                  <span className="px-2 py-0.5 bg-primary/10 text-primary text-xs rounded-full">
                    {article.category}
                  </span>
                  <span className="px-2 py-0.5 bg-primary/10 text-primary text-xs rounded-full flex items-center gap-1">
                    <CheckCircle className="w-3 h-3" />
                    Reviewed
                  </span>
                </div>
                <p className="text-sm text-textMuted mb-2">{article.content}</p>
                <div className="flex items-center gap-4 text-xs text-textMuted">
                  <span>{article.duration}</span>
                  <span>Updated: Jan 10, 2024</span>
                  <span className="text-primary">Source: WHO Guidelines</span>
                </div>
              </div>
              <div className="flex items-center gap-2 ml-4">
                <button className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                  <Edit3 className="w-4 h-4 text-textMuted" />
                </button>
                <button className="p-2 hover:bg-softWarning/10 rounded-lg transition-colors">
                  <Trash2 className="w-4 h-4 text-softWarning" />
                </button>
              </div>
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
