import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import SafetyBadge from '../../components/ui/SafetyBadge';
import { Phone, MessageCircle, AlertTriangle } from 'lucide-react';

export default function EmergencySupport() {
  return (
    <div className="min-h-screen bg-background pb-24 lg:pb-8">
      <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Header */}
        <motion.div
          className="mb-8 text-center"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-softWarning/10 flex items-center justify-center">
            <AlertTriangle className="w-8 h-8 text-softWarning" />
          </div>
          <h1 className="text-2xl font-semibold text-textMain mb-2">
            Need immediate support?
          </h1>
          <p className="text-textMuted">
            If you're feeling unsafe, please reach out to trusted people or professionals.
          </p>
        </motion.div>

        {/* Important message */}
        <motion.div
          className="bg-gradient-to-br from-softWarning/10 to-softWarning/5 rounded-3xl p-6 mb-6 border border-softWarning/20"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <h2 className="font-semibold text-textMain mb-3">Your safety matters most</h2>
          <p className="text-textMuted leading-relaxed">
            MindBridge AI is not a crisis hotline. If you're in immediate danger or having thoughts 
            of harming yourself or others, please seek help immediately.
          </p>
        </motion.div>

        {/* Immediate actions */}
        <motion.div
          className="space-y-4 mb-8"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <h2 className="font-semibold text-textMain">Get help now</h2>

          {/* Emergency contacts */}
          <div className="space-y-3">
            <div className="bg-surface rounded-2xl p-4 shadow-soft border border-gray-100">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-xl bg-softWarning/10 flex items-center justify-center">
                  <Phone className="w-6 h-6 text-softWarning" />
                </div>
                <div className="flex-1">
                  <h3 className="font-medium text-textMain">Emergency Services</h3>
                  <p className="text-sm text-textMuted">For life-threatening emergencies</p>
                </div>
                <a 
                  href="tel:911" 
                  className="px-4 py-2 bg-softWarning text-white rounded-xl font-medium text-sm"
                >
                  Call 911
                </a>
              </div>
            </div>

            <div className="bg-surface rounded-2xl p-4 shadow-soft border border-gray-100">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center">
                  <Phone className="w-6 h-6 text-primary" />
                </div>
                <div className="flex-1">
                  <h3 className="font-medium text-textMain">National Suicide Prevention Lifeline</h3>
                  <p className="text-sm text-textMuted">24/7, free and confidential</p>
                </div>
                <a 
                  href="tel:988" 
                  className="px-4 py-2 bg-primary text-white rounded-xl font-medium text-sm"
                >
                  Call 988
                </a>
              </div>
            </div>

            <div className="bg-surface rounded-2xl p-4 shadow-soft border border-gray-100">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-xl bg-secondary/10 flex items-center justify-center">
                  <MessageCircle className="w-6 h-6 text-secondary" />
                </div>
                <div className="flex-1">
                  <h3 className="font-medium text-textMain">Crisis Text Line</h3>
                  <p className="text-sm text-textMuted">Text HOME to 741741</p>
                </div>
                <a 
                  href="sms:741741&body=HOME" 
                  className="px-4 py-2 bg-secondary text-white rounded-xl font-medium text-sm"
                >
                  Text now
                </a>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Talk to someone you trust */}
        <motion.div
          className="bg-surface rounded-3xl p-6 mb-8 shadow-soft border border-gray-100"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          <h2 className="font-semibold text-textMain mb-4">Talk to someone you trust</h2>
          <p className="text-textMuted mb-4">
            Consider reaching out to:
          </p>
          <ul className="space-y-3">
            {[
              'A family member or close friend',
              'A trusted teacher or counselor',
              'Your doctor or healthcare provider',
              'A religious or spiritual leader',
            ].map((item, i) => (
              <li key={i} className="flex items-center gap-3 text-textMuted">
                <div className="w-2 h-2 rounded-full bg-primary" />
                {item}
              </li>
            ))}
          </ul>
        </motion.div>

        {/* Return to app */}
        <motion.div
          className="text-center"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
        >
          <Link
            to="/app"
            className="inline-flex items-center gap-2 text-primary font-medium"
          >
            Return to MindBridge AI
          </Link>
        </motion.div>

        {/* Safety note */}
        <motion.div
          className="mt-8"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.5 }}
        >
          <SafetyBadge variant="compact" />
        </motion.div>
      </div>
    </div>
  );
}
