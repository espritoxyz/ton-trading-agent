# How to Add Your Screenshots

The landing page currently shows placeholders where your screenshots should appear. Follow these steps to add the real images:

## Step 1: Prepare Your Screenshots

You need two screenshots:

### 1. Chat Interface Demo (`chat-demo.png`)
- Open http://localhost:5173 (the main Esprito AI app)
- Start a swap conversation, for example: "I want to swap 1 TON to USDT"
- Let the conversation complete including the confirmation step
- Take a screenshot of the entire chat panel showing:
  - User message
  - AI response with swap details
  - Confirmation buttons
  - Final "Sending..." or completion status
- Recommended size: 1920x1080 or similar

### 2. Tonviewer Transaction (`tonviewer-demo.png`)
- After completing a swap in the app, copy the transaction link
- Open the transaction on tonviewer.com
- Wait for "Confirmed transaction" status (green checkmark)
- Take a screenshot showing:
  - "Confirmed transaction" header
  - Transaction tree/flow diagram
  - Swap details (amounts, DEX used)
- Recommended size: 1400x900 or similar

## Step 2: Save Screenshots

1. Save your chat screenshot as: `chat-demo.png`
2. Save your Tonviewer screenshot as: `tonviewer-demo.png`

## Step 3: Add to Landing Page

Copy both images to the landing page images folder:

```bash
# From your Ton_agent directory:
cp /path/to/your/chat-demo.png landing-page/public/images/
cp /path/to/your/tonviewer-demo.png landing-page/public/images/
```

## Step 4: Verify

1. The landing page at http://localhost:3000 will auto-reload
2. Your screenshots should now appear in:
   - Hero section (chat-demo.png)
   - Tonviewer verification section (tonviewer-demo.png)

## Optional: Add Space Background

For the space/asteroid background:

1. Download a high-quality space image (like the asteroid one you showed)
2. Save it as `space-asteroid.jpg`
3. Copy to: `landing-page/public/images/space-asteroid.jpg`
4. Uncomment the space background in App.vue (search for "Space background image")

## Example Commands

```bash
# Navigate to your screenshots location
cd ~/Downloads  # or wherever you saved screenshots

# Copy to landing page
cp chat-interface-screenshot.png ~/Ton_agent/landing-page/public/images/chat-demo.png
cp tonviewer-screenshot.png ~/Ton_agent/landing-page/public/images/tonviewer-demo.png

# Optional: Add space background
cp space-asteroid-image.jpg ~/Ton_agent/landing-page/public/images/space-asteroid.jpg
```

## Image Optimization Tips

For best results:
- Use PNG format for screenshots (preserves quality)
- Keep file sizes under 1MB (compress if needed)
- Ensure dark theme is enabled in both apps
- Crop out any sensitive information (wallet addresses, etc.)
