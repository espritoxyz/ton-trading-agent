# Newsletter Content Format Guide

This document describes how to compose and send newsletters via the Esprito AI admin panel. It is intended for both human administrators and AI agents generating newsletter content.

---

## How It Works

The newsletter system accepts two fields:

| Field | Type | Description |
|---|---|---|
| `subject` | `string` | Email subject line (shown in inbox and browser tab) |
| `htmlContent` | `string` | The content body of the newsletter — HTML fragment |

The `htmlContent` you provide is injected **as-is** into a pre-built branded email template. You do **not** provide a full HTML page — only the inner content fragment.

### What the template provides automatically

- Esprito AI branded header (purple-to-violet gradient, logo)
- Responsive layout (600px max-width, mobile-friendly)
- Dark mode support (`prefers-color-scheme: dark`)
- Footer with copyright and a per-subscriber unsubscribe link
- Outlook/MSO compatibility layer

You only write the **body copy** that goes between the header and the footer.

---

## Admin API Endpoints

Both endpoints require an authenticated session with the `ADMIN` Keycloak role.

### Preview (returns rendered HTML, does not send)

```
POST /newsletter/admin/preview
Content-Type: application/json
Authorization: Bearer <token>

{
  "subject": "Your email subject",
  "htmlContent": "<p>Your content here</p>"
}
```

Returns `text/html` — the fully rendered email you can open in a browser to review.

### Send to all active subscribers

```
POST /newsletter/admin/send
Content-Type: application/json
Authorization: Bearer <token>

{
  "subject": "Your email subject",
  "htmlContent": "<p>Your content here</p>"
}
```

Returns:
```json
{
  "totalSubscribers": 342,
  "sent": 340,
  "failed": 2
}
```

---

## htmlContent Format

### The injection point

Your content is placed inside this container in the template:

```html
<div style="color: #1a1a1a; font-size: 16px; line-height: 1.7;">
    <!-- YOUR htmlContent GOES HERE -->
</div>
```

This means:
- Do **not** include `<html>`, `<head>`, `<body>`, or `<style>` tags
- Do **not** include `<script>` tags
- Write content as if you are writing the inside of a `<div>`

### Use inline styles

Email clients strip external stylesheets. All styling must be **inline** via the `style` attribute.

---

## Recommended HTML Elements and Patterns

### Headings

```html
<h2 style="color: #1a1a1a; font-size: 22px; font-weight: 700; margin: 0 0 16px 0; line-height: 1.3;">
  What's New in February
</h2>
```

Use `<h2>` and `<h3>` for section headings (the template header already acts as `<h1>`).

### Body text / paragraphs

```html
<p style="margin: 0 0 16px 0;">
  This month we launched limit orders on the TON blockchain. You can now set your target price
  and let Esprito AI execute the trade automatically when the market hits it.
</p>
```

### Bold and emphasis

```html
<p style="margin: 0 0 16px 0;">
  Trading is now <strong>3× faster</strong> thanks to improved transaction batching.
  You can also <em>schedule trades</em> for any time of day.
</p>
```

### Hyperlinks

```html
<a href="https://esprito.ai" style="color: #6366f1; text-decoration: underline;">
  Visit the platform
</a>
```

Use `#6366f1` (Esprito indigo) as the link color for brand consistency.

### Call-to-action button

```html
<table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin: 24px 0;">
  <tr>
    <td style="border-radius: 8px; background-color: #6366f1;">
      <a href="https://esprito.ai"
         style="display: inline-block; padding: 14px 32px; color: #ffffff; font-size: 16px;
                font-weight: 600; text-decoration: none; border-radius: 8px;
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;">
        Open Esprito AI
      </a>
    </td>
  </tr>
</table>
```

> Using a `<table>` wrapper for buttons ensures correct rendering in Outlook.

### Horizontal divider

```html
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0"
       style="margin: 24px 0;">
  <tr>
    <td style="border-top: 1px solid #e2e8f0;"></td>
  </tr>
</table>
```

### Highlighted info box

```html
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0"
       style="background-color: #eef2ff; border-left: 4px solid #6366f1; border-radius: 8px; margin: 24px 0;">
  <tr>
    <td style="padding: 16px 20px; color: #3730a3; font-size: 14px; line-height: 1.6;">
      <strong>Tip:</strong> You can now ask the AI agent to "swap all my TON to USDT" —
      it will calculate slippage and confirm before executing.
    </td>
  </tr>
</table>
```

### Bullet list

```html
<ul style="margin: 0 0 16px 0; padding-left: 20px; color: #1a1a1a;">
  <li style="margin-bottom: 8px;">Limit orders for TON/USDT and TON/NOT pairs</li>
  <li style="margin-bottom: 8px;">Price alert notifications via Telegram</li>
  <li style="margin-bottom: 8px;">Improved swap route optimization on Ston.fi</li>
</ul>
```

---

## Complete Example

Below is a full `htmlContent` value for a feature announcement newsletter:

```html
<h2 style="color: #1a1a1a; font-size: 22px; font-weight: 700; margin: 0 0 16px 0; line-height: 1.3;">
  Limit Orders Are Live on Esprito AI
</h2>

<p style="margin: 0 0 16px 0;">
  We're excited to announce that <strong>limit orders</strong> are now available for all Esprito AI users.
  Set your target price, and our AI agent will monitor the market and execute the trade automatically
  when the conditions are met — no need to stay glued to the charts.
</p>

<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0"
       style="background-color: #eef2ff; border-left: 4px solid #6366f1; border-radius: 8px; margin: 0 0 24px 0;">
  <tr>
    <td style="padding: 16px 20px; color: #3730a3; font-size: 14px; line-height: 1.6;">
      <strong>How to place a limit order:</strong> Just tell the agent — "Buy 100 USDT of NOT when the price drops below 0.008 TON."
    </td>
  </tr>
</table>

<h3 style="color: #1a1a1a; font-size: 17px; font-weight: 600; margin: 0 0 12px 0;">
  What's included in this release
</h3>

<ul style="margin: 0 0 24px 0; padding-left: 20px; color: #1a1a1a;">
  <li style="margin-bottom: 8px;">Limit buy and sell orders via natural language</li>
  <li style="margin-bottom: 8px;">Order status tracking in the transaction history</li>
  <li style="margin-bottom: 8px;">Automatic cancellation on expiry</li>
</ul>

<table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin: 0 0 24px 0;">
  <tr>
    <td style="border-radius: 8px; background-color: #6366f1;">
      <a href="https://esprito.app"
         style="display: inline-block; padding: 14px 32px; color: #ffffff; font-size: 16px;
                font-weight: 600; text-decoration: none; border-radius: 8px;
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;">
        Try Limit Orders Now
      </a>
    </td>
  </tr>
</table>

<p style="margin: 0; color: #64748b; font-size: 14px;">
  As always, the AI agent will ask for your confirmation before executing any trade.
  Your funds stay under your control.
</p>
```

---

## Subject Line Guidelines

- Keep it under **50 characters** for best display across email clients
- Be specific and direct — avoid vague subjects like "Monthly Update"
- Do not use ALL CAPS or excessive punctuation

**Good examples:**
- `Limit orders are live on Esprito AI`
- `New: Price alerts + Telegram notifications`
- `Your February trading summary is ready`

**Avoid:**
- `IMPORTANT ANNOUNCEMENT!!!`
- `Newsletter #12`
- `Updates` (too generic)

---

## Checklist Before Sending

- [ ] Reviewed the rendered output via admin panel preview
- [ ] All links use `https://` and point to the correct destination
- [ ] No `<html>`, `<head>`, `<body>`, or `<script>` tags in `htmlContent`
- [ ] All styles are inline (`style="..."`)
- [ ] Subject line is clear and under 50 characters
- [ ] Content is factually accurate and reviewed

---

## Brand Color Reference

| Usage | Hex |
|---|---|
| Primary / CTA buttons | `#6366f1` |
| Primary dark (hover) | `#4f52d8` |
| Secondary accent | `#a855f7` |
| Link color | `#6366f1` |
| Info box background | `#eef2ff` |
| Info box text | `#3730a3` |
| Body text | `#1a1a1a` |
| Muted text | `#64748b` |
| Divider / border | `#e2e8f0` |
