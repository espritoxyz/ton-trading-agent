# Sound Effects for Roadmap

Add subtle sound effects for an immersive space experience.

## Required Sound Files:

1. **click.mp3** - Click/selection sound (short, ~0.1s)
2. **hover.mp3** - Hover sound (very subtle, ~0.05s)
3. **whoosh.mp3** - Modal open/close (~0.3s)

## Recommended Sources:

### Free Sound Libraries
- https://freesound.org/ (search: "ui click", "space whoosh")
- https://mixkit.co/free-sound-effects/
- https://www.zapsplat.com/

### Specifications:
- **Format**: MP3 or OGG
- **Duration**: Very short (0.05s - 0.3s)
- **Volume**: Low/subtle (will be controlled by code)
- **Quality**: 128kbps MP3 is sufficient

## Creating Your Own:

You can use these tools to create custom sounds:
- **Audacity** (free) - record and edit
- **SFXR** (free) - generate retro game sounds
- **Online generators** - search "UI sound generator"

## Setup:

```bash
# Add your sound files to this directory
cp /path/to/sounds/*.mp3 /Users/igorkudinov/Ton_agent/landing-page/public/sounds/
```

## Sound Design Tips:

- **Click**: Short, crisp, sci-fi beep
- **Hover**: Very subtle, high-pitched tone
- **Whoosh**: Smooth swoosh for transitions

The sounds will be muted by default with a toggle button for users.
