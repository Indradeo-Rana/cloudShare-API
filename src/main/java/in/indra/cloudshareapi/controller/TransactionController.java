package in.indra.cloudshareapi.controller;

import in.indra.cloudshareapi.document.PaymentTransaction;
import in.indra.cloudshareapi.document.ProfileDocument;
import in.indra.cloudshareapi.repository.PaymentTransactionRepository;
import in.indra.cloudshareapi.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transaction")
@RequiredArgsConstructor
public class TransactionController { // Returning transaction history of the currently logged-in user

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<?> getUserTransaction(){
        try {
            ProfileDocument currentProfile = profileService.getCurrentProfile();
            String clerkId = currentProfile.getClerkId();

            List<PaymentTransaction> transactions = paymentTransactionRepository.findByClerkIdAndStatusOrderByTransactionDateDesc(clerkId, "SUCCESS");
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching transaction history: " + e.getMessage() + "...");
        }
    }
}
