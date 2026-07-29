import { useState } from 'react';
import { motion } from 'framer-motion';
import MoodOrb from '../../components/ui/MoodOrb';
import SafetyBadge from '../../components/ui/SafetyBadge';
import { moodOptions } from '../../data';
import { Moon, Battery, Check } from 'lucide-react';

export default function MoodCheckIn() {
  const [selectedMood, setSelectedMood] = useState<string | null>(null);
  const [stressLevel, setStressLevel] = useState(5);
  const [sleepQuality, setSleepQuality] = useState(5);
  const [energyLevel, setEnergyLevel] = useState(5);
  const [note, setNote] = useState('');
  const [isSubmitted, setIsSubmitted] = useState(false);

  const handleSubmit = () => {
    if (!selectedMood) return;
    setIsSubmitted(true);
    setTimeout(() => setIsSubmitted(false), 3000);
  };

  return (
    <div className="min-h-screen bg-background pb-24 lg:pb-8">
      <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Header */}
        <motion.div
          className="mb-8"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <h1 className="text-2xl font-semibold text-textMain mb-1">
            How are you feeling?
          </h1>
          <p className="text-textMuted">Take a moment to check in with yourself.</p>
        </motion.div>

        {isSubmitted ? (
          <motion.div
            className="bg-surface rounded-3xl p-8 shadow-soft text-center"
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
          >
            <motion.div
              className="w-20 h-20 mx-auto mb-6 rounded-full bg-primary/10 flex items-center justify-center"
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: 'spring', damping: 10 }}
            >
              <Check className="w-10 h-10 text-primary" />
            </motion.div>
            <h2 className="text-xl font-semibold text-textMain mb-2">Check-in saved</h2>
            <p className="text-textMuted">
              Your feelings are valid. Thank you for taking the time to reflect.
            </p>
          </motion.div>
        ) : (
          <div className="space-y-8">
            {/* Mood selection */}
            <motion.div
              className="bg-surface rounded-3xl p-6 shadow-soft"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
            >
              <h2 className="font-medium text-textMain mb-4">Select your mood</h2>
              <div className="grid grid-cols-4 gap-4">
                {moodOptions.map((mood) => (
                  <MoodOrb
                    key={mood.type}
                    mood={mood}
                    selected={selectedMood === mood.type}
                    onClick={() => setSelectedMood(mood.type)}
                    size="md"
                  />
                ))}
              </div>
            </motion.div>

            {/* Stress level */}
            <motion.div
              className="bg-surface rounded-3xl p-6 shadow-soft"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
            >
              <div className="flex items-center justify-between mb-4">
                <h2 className="font-medium text-textMain">Stress level</h2>
                <span className="text-primary font-semibold">{stressLevel}/10</span>
              </div>
              <input
                type="range"
                min="1"
                max="10"
                value={stressLevel}
                onChange={(e) => setStressLevel(Number(e.target.value))}
                className="w-full h-2 bg-surfaceMuted rounded-full appearance-none cursor-pointer slider-primary"
              />
              <div className="flex justify-between text-xs text-textMuted mt-2">
                <span>Low</span>
                <span>High</span>
              </div>
            </motion.div>

            {/* Sleep quality */}
            <motion.div
              className="bg-surface rounded-3xl p-6 shadow-soft"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
            >
              <div className="flex items-center gap-3 mb-4">
                <Moon className="w-5 h-5 text-secondary" />
                <h2 className="font-medium text-textMain">Sleep quality</h2>
                <span className="text-secondary font-semibold ml-auto">{sleepQuality}/10</span>
              </div>
              <input
                type="range"
                min="1"
                max="10"
                value={sleepQuality}
                onChange={(e) => setSleepQuality(Number(e.target.value))}
                className="w-full h-2 bg-surfaceMuted rounded-full appearance-none cursor-pointer slider-secondary"
              />
              <div className="flex justify-between text-xs text-textMuted mt-2">
                <span>Poor</span>
                <span>Excellent</span>
              </div>
            </motion.div>

            {/* Energy level */}
            <motion.div
              className="bg-surface rounded-3xl p-6 shadow-soft"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.35 }}
            >
              <div className="flex items-center gap-3 mb-4">
                <Battery className="w-5 h-5 text-accent" />
                <h2 className="font-medium text-textMain">Energy level</h2>
                <span className="text-accent font-semibold ml-auto">{energyLevel}/10</span>
              </div>
              <input
                type="range"
                min="1"
                max="10"
                value={energyLevel}
                onChange={(e) => setEnergyLevel(Number(e.target.value))}
                className="w-full h-2 bg-surfaceMuted rounded-full appearance-none cursor-pointer slider-accent"
              />
              <div className="flex justify-between text-xs text-textMuted mt-2">
                <span>Exhausted</span>
                <span>Energized</span>
              </div>
            </motion.div>

            {/* Journal note */}
            <motion.div
              className="bg-surface rounded-3xl p-6 shadow-soft"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
            >
              <h2 className="font-medium text-textMain mb-3">Anything on your mind?</h2>
              <p className="text-sm text-textMuted mb-4">Optional: Write a few words about how you're feeling.</p>
              <textarea
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="Today I feel..."
                className="w-full h-32 px-4 py-3 bg-surfaceMuted rounded-2xl text-textMain placeholder:text-textMuted/60 focus:outline-none focus:ring-2 focus:ring-primary/20 resize-none"
              />
            </motion.div>

            {/* Submit button */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.5 }}
            >
              <motion.button
                className={`w-full py-4 rounded-2xl font-medium text-lg transition-all ${
                  selectedMood
                    ? 'bg-primary text-white hover:bg-primaryDark shadow-glow'
                    : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                }`}
                onClick={handleSubmit}
                disabled={!selectedMood}
                whileHover={selectedMood ? { scale: 1.02 } : {}}
                whileTap={selectedMood ? { scale: 0.98 } : {}}
              >
                Save check-in
              </motion.button>
            </motion.div>

            {/* Safety badge */}
            <motion.div
              className="flex justify-center"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.6 }}
            >
              <SafetyBadge variant="compact" />
            </motion.div>
          </div>
        )}
      </div>

      <style>{`
        .slider-primary::-webkit-slider-thumb {
          appearance: none;
          width: 20px;
          height: 20px;
          background: #5F9E97;
          border-radius: 50%;
          cursor: pointer;
        }
        .slider-secondary::-webkit-slider-thumb {
          appearance: none;
          width: 20px;
          height: 20px;
          background: #6F86A6;
          border-radius: 50%;
          cursor: pointer;
        }
        .slider-accent::-webkit-slider-thumb {
          appearance: none;
          width: 20px;
          height: 20px;
          background: #D8C7A8;
          border-radius: 50%;
          cursor: pointer;
        }
      `}</style>
    </div>
  );
}
