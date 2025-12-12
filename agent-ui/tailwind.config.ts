import type { Config } from 'tailwindcss'

export default {
    content: ['./index.html', './src/**/*.{vue,ts}'],
    darkMode: 'class',
    theme: {
        extend: {
            fontFamily: {
                mono: ['IBM Plex Mono', 'monospace']
            }
        }
    },
    plugins: []
} satisfies Config
