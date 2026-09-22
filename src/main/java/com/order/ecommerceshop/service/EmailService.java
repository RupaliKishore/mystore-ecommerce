package com.order.ecommerceshop.service;

import com.order.ecommerceshop.model.Order;
import com.order.ecommerceshop.model.OrderItem;
import com.order.ecommerceshop.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService
{
    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ============================================================
    // PASSWORD RESET EMAIL (existing)
    // ============================================================
    public void sendPasswordResetEmail(String toEmail, String token)
    {
        String resetLink = frontendUrl + "/?token=" + token;
        String subject = "MyStore - Password Reset Request";
        String body = "Hello,\n\n" +
                "You requested to reset your password for MyStore.\n\n" +
                "Click the link below to reset your password:\n\n" +
                resetLink + "\n\n" +
                "This link will expire in 15 minutes.\n\n" +
                "If you didn't request this, please ignore this email.\n\n" +
                "Thanks,\nMyStore Team";

        sendEmail(toEmail, subject, body, resetLink);
    }

    // ============================================================
    // WELCOME EMAIL (NEW) — Register kelyavar
    // ============================================================
    @Async
    public void sendWelcomeEmail(User user)
    {
        String subject = "Welcome to MyStore, " + user.getName() + "! 🎉";
        String body = "Hi " + user.getName() + ",\n\n" +
                "Welcome to MyStore! We're excited to have you on board.\n\n" +
                "Here's what you can do:\n" +
                "✅ Browse thousands of products\n" +
                "🛒 Add items to your cart\n" +
                "❤️ Save favorites\n" +
                "📦 Track your orders in real-time\n" +
                "⭐ Write reviews and ratings\n\n" +
                "Start shopping now: " + frontendUrl + "\n\n" +
                "If you have any questions, feel free to reach out.\n\n" +
                "Happy Shopping!\n" +
                "The MyStore Team";

        sendEmail(user.getEmail(), subject, body, null);
    }

    // ============================================================
    // ORDER CONFIRMATION EMAIL (NEW)
    // ============================================================
    public void sendOrderConfirmationEmail(Order order)
    {
        String subject = "Order Confirmed - #" + order.getOrderNumber();
        String body = "Hi " + order.getUser().getName() + ",\n\n" +
                "Thank you for your order! 🎉\n\n" +
                "Order Details:\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "Order Number: #" + order.getOrderNumber() + "\n" +
                "Order Date: " + order.getOrderDate().format(DATE_FORMATTER) + "\n" +
                "Status: " + order.getStatus() + "\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "Items:\n" + buildItemsList(order) + "\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "Total Amount: ₹" + order.getTotalAmount() + "\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "Track your order: " + frontendUrl + "\n\n" +
                "We'll notify you when your order is shipped.\n\n" +
                "Thanks for shopping with us!\n" +
                "The MyStore Team";

        sendEmail(order.getUser().getEmail(), subject, body, null);
    }

    // ============================================================
    // ORDER STATUS UPDATE EMAIL (NEW)
    // ============================================================
    public void sendOrderStatusUpdateEmail(Order order)
    {
        String statusEmoji = getStatusEmoji(order.getStatus().name());
        String subject = statusEmoji + " Order #" + order.getOrderNumber() +
                " - " + order.getStatus();

        String statusMessage = getStatusMessage(order.getStatus().name());

        String body = "Hi " + order.getUser().getName() + ",\n\n" +
                statusMessage + "\n\n" +
                "Order Details:\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "Order Number: #" + order.getOrderNumber() + "\n" +
                "Current Status: " + order.getStatus() + "\n" +
                "Total Amount: ₹" + order.getTotalAmount() + "\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "Track your order: " + frontendUrl + "\n\n" +
                "Thanks for shopping with us!\n" +
                "The MyStore Team";

        sendEmail(order.getUser().getEmail(), subject, body, null);
    }

    // ============================================================
    // ORDER CANCELLATION EMAIL (NEW)
    // ============================================================
    public void sendOrderCancellationEmail(Order order)
    {
        String subject = "Order Cancelled - #" + order.getOrderNumber();
        String body = "Hi " + order.getUser().getName() + ",\n\n" +
                "Your order has been cancelled. ❌\n\n" +
                "Order Details:\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "Order Number: #" + order.getOrderNumber() + "\n" +
                "Total Amount: ₹" + order.getTotalAmount() + "\n" +
                "Status: CANCELLED\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "The stock for these items has been restored.\n\n" +
                "If you have any questions, please contact us.\n\n" +
                "Thanks,\n" +
                "The MyStore Team";

        sendEmail(order.getUser().getEmail(), subject, body, null);
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================
    private String buildItemsList(Order order)
    {
        StringBuilder sb = new StringBuilder();
        for (OrderItem item : order.getOrderItems())
        {
            BigDecimal itemTotal = item.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            sb.append("• ").append(item.getProduct().getName())
                    .append(" — Qty: ").append(item.getQuantity())
                    .append(" × ₹").append(item.getPrice())
                    .append(" = ₹").append(itemTotal)
                    .append("\n");
        }
        return sb.toString();
    }

    private String getStatusEmoji(String status)
    {
        return switch (status)
        {
            case "PENDING" -> "⏳";
            case "PROCESSING" -> "🔄";
            case "SHIPPED" -> "🚚";
            case "DELIVERED" -> "✅";
            case "CANCELLED" -> "❌";
            default -> "📦";
        };
    }

    private String getStatusMessage(String status)
    {
        return switch (status)
        {
            case "PENDING" -> "Your order has been received and is pending confirmation.";
            case "PROCESSING" -> "Great news! Your order is now being processed. 🔄";
            case "SHIPPED" -> "Your order has been shipped! It's on the way. 🚚";
            case "DELIVERED" -> "Your order has been delivered! We hope you love it. ✅";
            case "CANCELLED" -> "Your order has been cancelled.";
            default -> "Your order status has been updated.";
        };
    }

    // ============================================================
    // CORE SEND METHOD (with error handling)
    // ============================================================
    private void sendEmail(String toEmail, String subject, String body, String fallbackLink)
    {
        try
        {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(toEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(body);

            javaMailSender.send(mailMessage);
            log.info("✅ Email sent to {}: {}", toEmail, subject);
        }
        catch (Exception ex)
        {
            log.error("❌ Failed to send email to {}: {}", toEmail, ex.getMessage());
            if (fallbackLink != null) {
                log.info("🔗 Fallback link: {}", fallbackLink);
            }
        }
    }
}