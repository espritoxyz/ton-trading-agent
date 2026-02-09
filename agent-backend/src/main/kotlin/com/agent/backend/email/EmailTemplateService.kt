package com.agent.backend.email

import org.springframework.stereotype.Service

@Service
class EmailTemplateService {

    fun generateVerificationEmail(email: String, verificationLink: String, expiresInHours: Int): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Verify Your Email</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 600px;
            margin: 40px auto;
            background: white;
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 40px 20px;
            text-align: center;
        }
        .header h1 {
            margin: 0;
            color: white;
            font-size: 28px;
            font-weight: 600;
        }
        .content {
            padding: 40px 30px;
        }
        .content p {
            color: #333;
            font-size: 16px;
            line-height: 1.6;
            margin: 0 0 20px 0;
        }
        .button-container {
            text-align: center;
            margin: 30px 0;
        }
        .verify-button {
            display: inline-block;
            padding: 14px 40px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            text-decoration: none;
            border-radius: 8px;
            font-weight: 600;
            font-size: 16px;
            transition: transform 0.2s;
        }
        .verify-button:hover {
            transform: translateY(-2px);
        }
        .link-section {
            margin-top: 30px;
            padding: 20px;
            background-color: #f8f9fa;
            border-radius: 8px;
        }
        .link-section p {
            font-size: 14px;
            color: #666;
            margin-bottom: 10px;
        }
        .link-text {
            word-break: break-all;
            color: #667eea;
            font-size: 13px;
        }
        .footer {
            padding: 30px;
            text-align: center;
            color: #999;
            font-size: 13px;
            border-top: 1px solid #eee;
        }
        .expiry-notice {
            margin-top: 20px;
            padding: 15px;
            background-color: #fff3cd;
            border-left: 4px solid #ffc107;
            border-radius: 4px;
        }
        .expiry-notice p {
            color: #856404;
            font-size: 14px;
            margin: 0;
        }
        @media only screen and (max-width: 600px) {
            .container {
                margin: 20px;
            }
            .content {
                padding: 30px 20px;
            }
            .header h1 {
                font-size: 24px;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>✉️ Verify Your Email</h1>
        </div>
        <div class="content">
            <p>Hi there,</p>
            <p>Welcome to Esprito AI! We're excited to have you on board.</p>
            <p>To complete your registration and activate your account, please verify your email address by clicking the button below:</p>

            <div class="button-container">
                <a href="$verificationLink" class="verify-button">Verify Email Address</a>
            </div>

            <div class="link-section">
                <p>If the button doesn't work, copy and paste this link into your browser:</p>
                <p class="link-text">$verificationLink</p>
            </div>

            <div class="expiry-notice">
                <p><strong>⏰ Important:</strong> This verification link will expire in $expiresInHours hours. If it expires, you can request a new one from the login page.</p>
            </div>

            <p style="margin-top: 30px;">If you didn't create an account with Esprito AI, you can safely ignore this email.</p>
        </div>
        <div class="footer">
            <p>© 2026 Esprito AI. All rights reserved.</p>
            <p>This is an automated message. Please do not reply to this email.</p>
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }

    fun generatePasswordResetEmail(email: String, resetLink: String, expiresInHours: Int): String {
        // Placeholder for future implementation
        return ""
    }

    fun generateNewsletterEmail(content: String): String {
        // Placeholder for future implementation
        return ""
    }
}
