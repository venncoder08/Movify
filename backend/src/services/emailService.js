const nodemailer = require("nodemailer");
const env = require("../config/env");

let transporter;

/**
 * Initialize email transporter
 */
function initializeTransporter() {
  if (transporter) return transporter;

  // For Gmail or other services
  if (env.emailService === "gmail") {
    transporter = nodemailer.createTransport({
      service: "gmail",
      auth: {
        user: env.emailUser,
        pass: env.emailPassword // Use app password for Gmail, not regular password
      }
    });
  } else {
    // Generic SMTP configuration
    transporter = nodemailer.createTransport({
      host: env.emailHost,
      port: env.emailPort,
      secure: false, // true for 465, false for 587
      auth: {
        user: env.emailUser,
        pass: env.emailPassword
      }
    });
  }

  return transporter;
}

/**
 * Send OTP email
 * @param {string} toEmail - Recipient email
 * @param {string} otp - OTP code
 * @param {string} purpose - Purpose of OTP (e.g., "verify-email", "reset-password")
 * @returns {Promise<object>}
 */
async function sendOTPEmail(toEmail, otp, purpose = "verify-email") {
  try {
    if (!env.emailUser || !env.emailPassword) {
      console.error("Email service not configured");
      return {
        success: false,
        message: "Email service not configured"
      };
    }

    const transporter = initializeTransporter();

    const subject = {
      "verify-email": "Verify Your Movify Email",
      "reset-password": "Reset Your Movify Password",
      "two-factor": "Your Movify Two-Factor Authentication Code"
    }[purpose] || "Movify OTP Verification";

    const htmlTemplate = getEmailTemplate(otp, purpose);

    const mailOptions = {
      from: env.emailFrom,
      to: toEmail,
      subject,
      html: htmlTemplate,
      text: `Your OTP is: ${otp}. Valid for 10 minutes.`
    };

    const info = await transporter.sendMail(mailOptions);

    console.log("Email sent:", info.response);
    return {
      success: true,
      message: "OTP email sent successfully",
      messageId: info.messageId
    };
  } catch (error) {
    console.error("Error sending email:", error);
    return {
      success: false,
      message: error.message
    };
  }
}

/**
 * Get email HTML template
 * @param {string} otp - OTP code
 * @param {string} purpose - Purpose of OTP
 * @returns {string} HTML template
 */
function getEmailTemplate(otp, purpose) {
  const purposeText = {
    "verify-email": "Verify Your Email",
    "reset-password": "Reset Your Password",
    "two-factor": "Complete Two-Factor Authentication"
  }[purpose] || "OTP Verification";

  return `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <style>
        body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
        .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; }
        .header { text-align: center; border-bottom: 2px solid #007bff; padding-bottom: 20px; }
        .header h1 { color: #007bff; margin: 0; }
        .content { padding: 30px 0; text-align: center; }
        .otp-box { 
          background-color: #f0f0f0; 
          padding: 20px; 
          border-radius: 8px; 
          margin: 20px 0;
          border: 2px dashed #007bff;
        }
        .otp-code { 
          font-size: 32px; 
          font-weight: bold; 
          color: #007bff; 
          letter-spacing: 5px;
          font-family: monospace;
        }
        .message { color: #666; font-size: 14px; margin-top: 20px; }
        .warning { color: #dc3545; font-weight: bold; }
        .footer { 
          text-align: center; 
          border-top: 1px solid #ddd; 
          padding-top: 20px; 
          font-size: 12px; 
          color: #999; 
        }
      </style>
    </head>
    <body>
      <div class="container">
        <div class="header">
          <h1>🚗 Movify</h1>
        </div>
        <div class="content">
          <h2>${purposeText}</h2>
          <p>Your One-Time Password (OTP) for Movify is:</p>
          <div class="otp-box">
            <div class="otp-code">${otp}</div>
          </div>
          <div class="message">
            <p><strong>This OTP is valid for 10 minutes</strong></p>
            <p>Do not share this code with anyone. Movify staff will never ask for your OTP.</p>
            <p><span class="warning">⚠️ If you didn't request this code, please ignore this email.</span></p>
          </div>
        </div>
        <div class="footer">
          <p>&copy; 2024 Movify. All rights reserved.</p>
          <p>This is an automated email. Please do not reply to this address.</p>
        </div>
      </div>
    </body>
    </html>
  `;
}

/**
 * Send welcome email
 * @param {string} toEmail - Recipient email
 * @param {string} fullName - User full name
 * @returns {Promise<object>}
 */
async function sendWelcomeEmail(toEmail, fullName) {
  try {
    if (!env.emailUser || !env.emailPassword) {
      return {
        success: false,
        message: "Email service not configured"
      };
    }

    const transporter = initializeTransporter();

    const htmlTemplate = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <style>
          body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
          .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; }
          .header { text-align: center; border-bottom: 2px solid #007bff; padding-bottom: 20px; }
          .header h1 { color: #007bff; margin: 0; }
          .content { padding: 30px 0; }
          .footer { text-align: center; border-top: 1px solid #ddd; padding-top: 20px; font-size: 12px; color: #999; }
        </style>
      </head>
      <body>
        <div class="container">
          <div class="header">
            <h1>🚗 Welcome to Movify</h1>
          </div>
          <div class="content">
            <p>Hi ${fullName},</p>
            <p>Welcome to Movify! We're excited to have you on board.</p>
            <p>Your account has been successfully created. You can now:</p>
            <ul>
              <li>Book rides with our trusted drivers</li>
              <li>Track your ride in real-time</li>
              <li>Manage your profile and preferences</li>
              <li>Access exclusive offers and promotions</li>
            </ul>
            <p>If you have any questions, feel free to contact our support team.</p>
            <p>Happy riding!<br><strong>Movify Team</strong></p>
          </div>
          <div class="footer">
            <p>&copy; 2024 Movify. All rights reserved.</p>
          </div>
        </div>
      </body>
      </html>
    `;

    const mailOptions = {
      from: env.emailFrom,
      to: toEmail,
      subject: "Welcome to Movify! 🚗",
      html: htmlTemplate
    };

    const info = await transporter.sendMail(mailOptions);

    console.log("Welcome email sent:", info.response);
    return {
      success: true,
      message: "Welcome email sent successfully"
    };
  } catch (error) {
    console.error("Error sending welcome email:", error);
    return {
      success: false,
      message: error.message
    };
  }
}

/**
 * Test email configuration
 * @returns {Promise<object>}
 */
async function testEmailConfiguration() {
  try {
    if (!env.emailUser || !env.emailPassword) {
      return {
        success: false,
        message: "Email configuration not set"
      };
    }

    const transporter = initializeTransporter();
    await transporter.verify();

    return {
      success: true,
      message: "Email configuration is valid"
    };
  } catch (error) {
    console.error("Email configuration error:", error);
    return {
      success: false,
      message: error.message
    };
  }
}

module.exports = {
  sendOTPEmail,
  sendWelcomeEmail,
  testEmailConfiguration,
  initializeTransporter
};
