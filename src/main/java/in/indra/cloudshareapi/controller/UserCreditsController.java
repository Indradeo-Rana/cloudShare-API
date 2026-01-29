package in.indra.cloudshareapi.controller;

import in.indra.cloudshareapi.dto.UserCreditsDTO;
import in.indra.cloudshareapi.document.UserCredits;
import in.indra.cloudshareapi.service.UserCreditsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserCreditsController {

    private final UserCreditsService userCreditsService;

    @GetMapping("/credits")
    public ResponseEntity<UserCreditsDTO> getUserCredits() {

        UserCredits userCredits = userCreditsService.getUserCredits();

        UserCreditsDTO response = UserCreditsDTO.builder()
                .credits(userCredits.getCredits())
                .plan(userCredits.getPlan())
                .build();

        return ResponseEntity.ok(response);
    }
}
