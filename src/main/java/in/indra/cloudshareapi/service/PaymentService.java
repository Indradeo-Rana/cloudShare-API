package in.indra.cloudshareapi.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import in.indra.cloudshareapi.document.PaymentTransaction;
import in.indra.cloudshareapi.document.ProfileDocument;
import in.indra.cloudshareapi.dto.PaymentDTO;
import in.indra.cloudshareapi.dto.PaymentVerificationDTO;
import in.indra.cloudshareapi.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

import static ch.qos.logback.core.encoder.ByteArrayUtil.toHexString;
import static com.razorpay.Utils.verifySignature;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorKeySecret;

    /* ===========================
       CREATE ORDER
       =========================== */
    public PaymentDTO createOrder(PaymentDTO paymentDTO) {
        try {
            ProfileDocument currentProfile = profileService.getCurrentProfile();
            String clerkId = currentProfile.getClerkId();

            RazorpayClient razorpayClient =
                    new RazorpayClient(razorpayKeyId, razorKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", paymentDTO.getAmount());
            orderRequest.put("currency", paymentDTO.getCurrency());
            orderRequest.put("receipt", "order_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .clerkId(clerkId)
                    .orderId(orderId)
                    .planId(paymentDTO.getPlanId())
                    .amount(paymentDTO.getAmount())
                    .currency(paymentDTO.getCurrency())
                    .status("PENDING")
                    .transactionDate(LocalDateTime.now())
                    .userEmail(currentProfile.getEmail())
                    .userName(currentProfile.getFirstName() + " " + currentProfile.getLastName())
                    .build();

            paymentTransactionRepository.save(transaction);

            return PaymentDTO.builder()
                    .orderId(orderId)
                    .success(true)
                    .message("Order created successfully")
                    .build();

        } catch (Exception e) {
            return PaymentDTO.builder()
                    .success(false)
                    .message("Error creating order: " + e.getMessage())
                    .build();
        }
    }

    /* ===========================
       VERIFY PAYMENT
       =========================== */
    public PaymentDTO verifyPayment(PaymentVerificationDTO request) {

        try {
            ProfileDocument currentProfile = profileService.getCurrentProfile();
            String clerkId = currentProfile.getClerkId();

            // Correct Razorpay format (NO SPACES)
            String data =
                    request.getRazorpay_order_id() + "|" + request.getRazorpay_payment_id();

            String generatedSignature =
                    generateHmacSha256Signature(data, razorKeySecret);

            if (!generatedSignature.equals(request.getRazorpay_signature())) {
                updateTransactionStatus(
                        request.getRazorpay_order_id(),
                        "FAILED",
                        request.getRazorpay_payment_id(),
                        null
                );

                return PaymentDTO.builder()
                        .success(false)
                        .message("Payment verification failed")
                        .build();
            }

            int creditsToAdd = 0;
            String plan = "BASIC";

            switch (request.getPlanId()) {
                case "basic":
                    creditsToAdd = 5;
                    plan = "BASIC";
                    break;
                case "premium":
                    creditsToAdd = 500;
                    plan = "PREMIUM";
                    break;
                case "ultimate":
                    creditsToAdd = 5000;
                    plan = "ULTIMATE";
                    break;
            }

            if (creditsToAdd <= 0) {
                updateTransactionStatus(
                        request.getRazorpay_order_id(),
                        "FAILED",
                        request.getRazorpay_payment_id(),
                        null
                );

                return PaymentDTO.builder()
                        .success(false)
                        .message("Invalid plan selected")
                        .build();
            }

            // Add credits
            userCreditsService.addCredits(clerkId, creditsToAdd, plan);

            updateTransactionStatus(
                    request.getRazorpay_order_id(),
                    "SUCCESS",
                    request.getRazorpay_payment_id(),
                    creditsToAdd
            );

            return PaymentDTO.builder()
                    .success(true)
                    .message("Payment verified and credits added successfully")
                    .credits(userCreditsService.getUserCredits(clerkId).getCredits())
                    .build();

        } catch (Exception e) {
            updateTransactionStatus(
                    request.getRazorpay_order_id(),
                    "ERROR",
                    request.getRazorpay_payment_id(),
                    null
            );

            return PaymentDTO.builder()
                    .success(false)
                    .message("Payment verification failed. Please contact support")
                    .build();
        }
    }

    /* ===========================
       UPDATE TRANSACTION
       =========================== */
    private void updateTransactionStatus(
            String razorpayOrderId,
            String status,
            String razorpayPaymentId,
            Integer creditsAdded
    ) {
        paymentTransactionRepository.findAll().stream()
                .filter(t -> razorpayOrderId.equals(t.getOrderId()))
                .findFirst()
                .ifPresent(transaction -> {
                    transaction.setStatus(status);
                    transaction.setPaymentId(razorpayPaymentId);
                    if (creditsAdded != null) {
                        transaction.setCreditsAdded(creditsAdded);
                    }
                    paymentTransactionRepository.save(transaction);
                });
    }

    /* ===========================
       SIGNATURE GENERATION
       =========================== */
    private String generateHmacSha256Signature(String data, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {

        SecretKeySpec secretKey =
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKey);

        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        return bytesToHex(rawHmac);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}

