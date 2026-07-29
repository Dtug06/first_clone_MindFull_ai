/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        background: '#F7F4EE',
        surface: '#FFFFFF',
        surfaceMuted: '#EEF4F1',
        primary: '#5F9E97',
        primaryDark: '#3F7470',
        secondary: '#6F86A6',
        accent: '#D8C7A8',
        textMain: '#263238',
        textMuted: '#6E7772',
        softWarning: '#C8766B',
        lavenderMist: '#E8E4F2',
        oceanDeep: '#243B4A',
      },
      fontFamily: {
        sans: ['Inter', 'Manrope', 'Be Vietnam Pro', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        '2xl': '1rem',
        '3xl': '1.5rem',
        '4xl': '2rem',
      },
      boxShadow: {
        'soft': '0 2px 20px rgba(38, 50, 56, 0.06)',
        'soft-lg': '0 4px 40px rgba(38, 50, 56, 0.08)',
        'soft-xl': '0 8px 60px rgba(38, 50, 56, 0.1)',
        'glow': '0 0 40px rgba(95, 158, 151, 0.3)',
        'glow-sm': '0 0 20px rgba(95, 158, 151, 0.2)',
      },
      animation: {
        'float': 'float 6s ease-in-out infinite',
        'float-slow': 'float 8s ease-in-out infinite',
        'float-slower': 'float 10s ease-in-out infinite',
        'breathe': 'breathe 4s ease-in-out infinite',
        'pulse-soft': 'pulse-soft 3s ease-in-out infinite',
        'wave': 'wave 8s ease-in-out infinite',
        'slide-up': 'slideUp 0.5s ease-out',
        'slide-down': 'slideDown 0.5s ease-out',
        'fade-in': 'fadeIn 0.5s ease-out',
        'glow': 'glow 3s ease-in-out infinite',
        'tentacle': 'tentacle 6s ease-in-out infinite',
      },
      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0px) translateX(0px)' },
          '25%': { transform: 'translateY(-20px) translateX(10px)' },
          '50%': { transform: 'translateY(-10px) translateX(-10px)' },
          '75%': { transform: 'translateY(-30px) translateX(5px)' },
        },
        breathe: {
          '0%, 100%': { transform: 'scale(1)', opacity: '0.8' },
          '50%': { transform: 'scale(1.1)', opacity: '1' },
        },
        'pulse-soft': {
          '0%, 100%': { transform: 'scale(1)', opacity: '0.6' },
          '50%': { transform: 'scale(1.05)', opacity: '0.8' },
        },
        wave: {
          '0%, 100%': { transform: 'translateX(0) translateY(0)' },
          '50%': { transform: 'translateX(20px) translateY(-10px)' },
        },
        slideUp: {
          '0%': { transform: 'translateY(20px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
        slideDown: {
          '0%': { transform: 'translateY(-20px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        glow: {
          '0%, 100%': { boxShadow: '0 0 20px rgba(95, 158, 151, 0.3)' },
          '50%': { boxShadow: '0 0 40px rgba(95, 158, 151, 0.5)' },
        },
        tentacle: {
          '0%, 100%': { transform: 'rotate(-5deg) scaleY(1)' },
          '25%': { transform: 'rotate(5deg) scaleY(1.05)' },
          '50%': { transform: 'rotate(-3deg) scaleY(0.95)' },
          '75%': { transform: 'rotate(4deg) scaleY(1.02)' },
        },
      },
    },
  },
  plugins: [],
}
