# MindBridge AI

A calmer bridge to your inner world. AI-powered mental health support platform for young Vietnamese users.

## Overview

MindBridge AI is a comprehensive mental wellness platform that combines AI companionship, mood tracking, self-help resources, and professional support connections. The interface features a gentle, jellyfish-themed design language that promotes calmness and emotional well-being.

## Features

### Public Landing Page
- Hero section with floating jellyfish mascot
- Problem/pain point visualization
- Step-by-step "How It Works" guide
- Core features showcase
- Jellyfish companion introduction
- Safety & Privacy information
- Organization dashboard preview
- Call-to-action sections

### User App
- **Home/Daily Space**: Greeting, daily check-in, breathing orb, recommendations, mood trends
- **Mood Check-in**: Mood selection, stress/sleep/energy sliders, journal field
- **AI Companion Chat**: Gentle chat interface with suggested prompts
- **Mental Health Dashboard**: Mood trends, check-in frequency, insights, recommendations
- **Self-help Library**: Categorized exercises, search functionality
- **Emergency Support**: Calm crisis resources with contact information

### Admin Dashboard
- Overview with key metrics and charts
- User management with search and filters
- Risk monitoring with four-level system
- Expert management
- Content library management
- AI & Knowledge Base settings
- Organization dashboard (anonymous aggregates)
- Settings & Audit logs

### Expert Portal
- Assigned cases list
- Case details and notes

## Design System

### Colors
- Background: `#F7F4EE` (Warm ivory)
- Surface: `#FFFFFF` (White)
- Primary: `#5F9E97` (Soft sage)
- Primary Dark: `#3F7470`
- Secondary: `#6F86A6` (Muted blue)
- Accent: `#D8C7A8` (Sand beige)
- Text Main: `#263238` (Deep charcoal)
- Text Muted: `#6E7772`
- Soft Warning: `#C8766B` (Muted coral)
- Lavender Mist: `#E8E4F2`
- Ocean Deep: `#243B4A`

### Typography
- Font Family: Inter, Manrope, Be Vietnam Pro
- Headings: Soft but confident
- Body: Readable and calm

### Visual Style
- Large rounded cards
- Soft shadows
- Subtle borders
- Plenty of whitespace
- Gentle gradients
- Floating jellyfish animations
- Breathing orb animations
- Smooth page transitions

## Tech Stack

- **Framework**: React 18 + TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS
- **Animations**: Framer Motion
- **Icons**: Lucide React
- **Routing**: React Router DOM

## Getting Started

### Prerequisites
- Node.js 18+
- npm or yarn

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## Project Structure

```
src/
├── components/
│   ├── ui/           # Reusable UI components
│   │   ├── JellyfishMascot.tsx
│   │   ├── FloatingJellyfishBackground.tsx
│   │   ├── CalmCard.tsx
│   │   ├── SectionHeader.tsx
│   │   ├── AnimatedGradientBlob.tsx
│   │   ├── BreathingOrb.tsx
│   │   ├── MoodOrb.tsx
│   │   ├── SafetyBadge.tsx
│   │   ├── PrivacyBadge.tsx
│   │   ├── RecommendationCard.tsx
│   │   ├── DashboardMetricCard.tsx
│   │   ├── RiskLevelBadge.tsx
│   │   └── SoftLineChart.tsx
│   ├── layout/       # Layout components
│   │   ├── Sidebar.tsx
│   │   ├── Topbar.tsx
│   │   ├── MobileNavigation.tsx
│   │   └── PageTransitionWrapper.tsx
│   └── landing/      # Landing page sections
│       ├── HeroSection.tsx
│       ├── ProblemSection.tsx
│       ├── HowItWorksSection.tsx
│       ├── FeaturesSection.tsx
│       ├── JellyfishCompanionSection.tsx
│       ├── SafetyPrivacySection.tsx
│       ├── OrganizationsSection.tsx
│       ├── FinalCTASection.tsx
│       └── LandingNav.tsx
├── pages/
│   ├── user/         # User app pages
│   ├── admin/        # Admin dashboard pages
│   └── expert/       # Expert portal pages
├── data/             # Sample data and constants
├── types/            # TypeScript type definitions
└── App.tsx          # Main app with routing
```

## Routes

### Landing Page
- `/` - Public landing page

### User App
- `/app` - User home/dashboard
- `/app/check-in` - Mood check-in
- `/app/chat` - AI companion chat
- `/app/dashboard` - Mental health dashboard
- `/app/library` - Self-help library
- `/app/emergency` - Emergency support

### Admin Dashboard
- `/admin` - Overview
- `/admin/users` - User management
- `/admin/risk` - Risk monitoring
- `/admin/experts` - Expert management
- `/admin/content` - Content library
- `/admin/ai` - AI & Knowledge Base
- `/admin/organizations` - Organization dashboard
- `/admin/settings` - Settings & Logs

### Expert Portal
- `/expert` - Assigned cases
- `/expert/cases` - Case list

## Accessibility

- Semantic HTML elements
- ARIA labels where needed
- Keyboard navigation support
- Reduced motion support (`prefers-reduced-motion`)
- Color contrast compliance

## Important Notes

- This is a frontend implementation with placeholder data
- Backend APIs are not included
- Emergency contact numbers are placeholders
- AI chat responses are simulated
- All data is for demonstration purposes

## License

Proprietary - MindBridge AI Team 2024
