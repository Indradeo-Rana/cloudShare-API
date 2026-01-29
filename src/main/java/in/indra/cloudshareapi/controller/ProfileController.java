package in.indra.cloudshareapi.controller;

import in.indra.cloudshareapi.document.ProfileDocument;
import in.indra.cloudshareapi.dto.ProfileDTO;
import in.indra.cloudshareapi.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProfileController { // this class handles profile-related requests coming from frontend or external systems.

    private final ProfileService profileService;

    @GetMapping("/check") // It’s just a sanity check.
    public String hello(){
        System.out.println("ok");
        return "Welcome to CloudShare API";
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerProfile(@RequestBody ProfileDTO profileDTO) {
        //“Does this user already exist?”
        HttpStatus status = profileService.existsByClerkId(profileDTO.getClerkId()) ?
                HttpStatus.OK : HttpStatus.CREATED;

        ProfileDTO savedProfile = profileService.createProfile(profileDTO);
//        System.out.println("Profile created: " + savedProfile);
        return ResponseEntity.status(status).body(savedProfile);

    }

    // Ensure that a profile exists for the given clerkId; if not, create one. chatgpt
    @PostMapping("/profile/ensure")
    public ResponseEntity<?> ensureProfile(@RequestBody ProfileDTO dto) {

        ProfileDocument profile =
                profileService.ensureProfileExists(dto);

        return ResponseEntity.ok(profile);
    }
}
