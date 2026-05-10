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
        message: "Dịch vụ email chưa được cấu hình"
      };
    }

    const transporter = initializeTransporter();

    const subject = {
      "verify-email": "Xác thực email Movify",
      "reset-password": "Đặt lại mật khẩu Movify",
      "two-factor": "Mã xác thực hai lớp Movify"
    }[purpose] || "Xác thực OTP Movify";

    const htmlTemplate = getEmailTemplate(otp, purpose);

    const mailOptions = {
      from: env.emailFrom,
      to: toEmail,
      subject,
      html: htmlTemplate,
      text: `Mã OTP của bạn là: ${otp}. Có hiệu lực trong 10 phút.`
    };

    const info = await transporter.sendMail(mailOptions);

    console.log("Email sent:", info.response);
    return {
      success: true,
      message: "Gửi email OTP thành công",
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
    "verify-email": "Xác thực email",
    "reset-password": "Đặt lại mật khẩu",
    "two-factor": "Hoàn tất xác thực hai lớp"
  }[purpose] || "Xác thực OTP";

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
          <p>Mã OTP Movify của bạn là:</p>
          <div class="otp-box">
            <div class="otp-code">${otp}</div>
          </div>
          <div class="message">
            <p><strong>Mã OTP này có hiệu lực trong 10 phút</strong></p>
            <p>Không chia sẻ mã này với bất kỳ ai. Nhân viên Movify sẽ không bao giờ yêu cầu mã OTP của bạn.</p>
            <p><span class="warning">⚠️ Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email.</span></p>
          </div>
        </div>
        <div class="footer">
          <p>&copy; 2024 Movify. Đã đăng ký bản quyền.</p>
          <p>Đây là email tự động. Vui lòng không trả lời địa chỉ này.</p>
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
        message: "Dịch vụ email chưa được cấu hình"
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
            <h1>🚗 Chào mừng đến với Movify</h1>
          </div>
          <div class="content">
            <p>Xin chào ${fullName},</p>
            <p>Chào mừng bạn đến với Movify! Rất vui khi có bạn đồng hành.</p>
            <p>Tài khoản của bạn đã được tạo thành công. Bạn có thể:</p>
            <ul>
              <li>Đặt xe với các tài xế đáng tin cậy</li>
              <li>Theo dõi chuyến đi theo thời gian thực</li>
              <li>Quản lý hồ sơ và tùy chọn cá nhân</li>
              <li>Nhận các ưu đãi và khuyến mãi dành riêng cho bạn</li>
            </ul>
            <p>Nếu có câu hỏi, bạn có thể liên hệ đội ngũ hỗ trợ của Movify.</p>
            <p>Chúc bạn có những chuyến đi thật thuận tiện!<br><strong>Đội ngũ Movify</strong></p>
          </div>
          <div class="footer">
            <p>&copy; 2024 Movify. Đã đăng ký bản quyền.</p>
          </div>
        </div>
      </body>
      </html>
    `;

    const mailOptions = {
      from: env.emailFrom,
      to: toEmail,
      subject: "Chào mừng đến với Movify! 🚗",
      html: htmlTemplate
    };

    const info = await transporter.sendMail(mailOptions);

    console.log("Welcome email sent:", info.response);
    return {
      success: true,
      message: "Gửi email chào mừng thành công"
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
        message: "Chưa thiết lập cấu hình email"
      };
    }

    const transporter = initializeTransporter();
    await transporter.verify();

    return {
      success: true,
      message: "Cấu hình email hợp lệ"
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
