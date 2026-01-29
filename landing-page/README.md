# Esprito AI Landing Page

A futuristic, cosmos-themed landing page for the Esprito AI trading agent platform.

## Features

- 🌌 **Cosmic Design** - Space-themed visuals with animated stars and gradient orbs
- 🚀 **Hero Section** - Compelling introduction with chat mockup
- ⚡ **Features Showcase** - 6 key features with animated cards
- 💎 **TON Blockchain Focus** - Dedicated section highlighting TON support
- 🗺️ **Roadmap** - Multi-chain expansion timeline
- 📧 **Email Subscription** - Newsletter signup form
- 🔗 **Social Links** - X, Telegram, Reddit, Instagram
- 📄 **Legal Footer** - Terms, Privacy Policy, About Us

## Tech Stack

- **Vue 3** - Progressive JavaScript framework
- **Vite** - Next-generation frontend build tool
- **Tailwind CSS** - Utility-first CSS framework
- **Custom animations** - Floating elements, glows, and cosmic effects

## Getting Started

### Installation

```bash
cd landing-page
npm install
```

### Development

Run the development server:

```bash
npm run dev
```

The landing page will be available at [http://localhost:3000](http://localhost:3000)

### Build for Production

```bash
npm run build
```

The production-ready files will be in the `dist` directory.

### Preview Production Build

```bash
npm run preview
```

## Customization

### Colors

The cosmic theme colors are defined in `tailwind.config.js`:
- `cosmic.*` - Main purple/indigo shades
- `space.*` - Dark background colors

### Social Links

Update social media URLs in `src/App.vue`:
- Twitter/X: Line ~290
- Telegram: Line ~305
- Reddit: Line ~320
- Instagram: Line ~335

### Email Subscription

The email form currently uses a mock implementation (setTimeout). To integrate with a real email service:

1. Add your email service API endpoint
2. Update the `handleSubscribe` function in `src/App.vue` (around line 9-18)
3. Example services: Mailchimp, SendGrid, ConvertKit

### Legal Pages

Create separate pages for:
- Terms of Service
- Privacy Policy
- Cookie Policy
- About Us

Update the footer links in `src/App.vue` to point to these pages.

## Deployment

### Vercel

```bash
npm install -g vercel
vercel
```

### Netlify

```bash
npm install -g netlify-cli
netlify deploy
```

### Custom Server

Build the project and serve the `dist` directory with any static file server.


## License

Copyright © 2026 Esprito Tech QFZ LLC. All rights reserved.
