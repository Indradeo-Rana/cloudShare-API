package in.indra.cloudshareapi.controller;

import in.indra.cloudshareapi.dto.PaymentDTO;
import in.indra.cloudshareapi.dto.PaymentVerificationDTO;
import in.indra.cloudshareapi.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

//    User clicks Buy(Purchase) Plan & Frontend sends plan details (amount, planId, credits)
//    @PostMapping("/create-order")
//    public ResponseEntity<?> createOrder (@RequestBody PaymentDTO paymentDTO){
//        // call service method to create order
//        PaymentDTO response = paymentService.createOrder(paymentDTO);
//
//        if(response.getSuccess()) return ResponseEntity.ok(response);
//        else return ResponseEntity.badRequest().body(response);
//    }

//    @PostMapping("/verify-payment")
//    public ResponseEntity<?> verifyPayment(@RequestBody PaymentVerificationDTO request){
//     // use service method
//    PaymentDTO response = paymentService.verifyPayment(request);
//
//    if(response.getSuccess()) return ResponseEntity.ok(response);
//    else  return ResponseEntity.badRequest().body(response);
//    }

    @PostMapping("/create-order")
    public ResponseEntity<PaymentDTO> createOrder(@RequestBody PaymentDTO paymentDTO) {
        PaymentDTO response = paymentService.createOrder(paymentDTO);

        if (Boolean.TRUE.equals(response.getSuccess())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<PaymentDTO> verifyPayment( @RequestBody PaymentVerificationDTO request) {
        PaymentDTO response = paymentService.verifyPayment(request);

        if (Boolean.TRUE.equals(response.getSuccess())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}
