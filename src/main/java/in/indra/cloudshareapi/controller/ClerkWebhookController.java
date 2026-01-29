package in.indra.cloudshareapi.controller;

import in.indra.cloudshareapi.dto.ProfileDTO;
import in.indra.cloudshareapi.service.ProfileService;
import in.indra.cloudshareapi.service.UserCreditsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class ClerkWebhookController {

    @Value("${clerk.webhook.secret}")
    private String webhookSecret;

    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;

    @PostMapping("/clerk")
    public ResponseEntity<?> handleClerkWebhook(@RequestHeader("svix-id") String svixId,
                                                @RequestHeader("svix-timestamp") String svixTimestamp,
                                                @RequestHeader("svix-signature") String svixSignature,
                                                @RequestBody String payload) {
        try {
            boolean isValid = verifyWebhookSignature(svixId, svixSignature, svixTimestamp, payload);
            if (!isValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook signature...");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(payload); // parse payload
            String eventType = rootNode.path("type").asText(); // get event type

            switch(eventType){
                case "user.created":
                    // Handle profile created event
                    handleUserCreate(rootNode.path("data"));
                    break;
                case "user.updated":
                    // Handle profile updated event
                    handleUserUpdated(rootNode.path("data"));
                    break;
                case "user.deleted":
                    // Handle profile deleted event
                    handleUserDeleted(rootNode.path("data"));
                    break;
            }
            return ResponseEntity.ok().build();
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());

        }
    }

    private void handleUserDeleted(JsonNode data) {
        String clerkId = data.path("id").asText();

        profileService.deleteProfile(clerkId);
    }

    private void handleUserUpdated(JsonNode data) {
        String clerkId = data.path("id").asText();

        String email="";
        JsonNode emailAddresses =data.path("email_addresses");
        if(emailAddresses.isArray() && emailAddresses.size() > 0){
            email = emailAddresses.get(0).path("email_address").asText();
        }

        String firstName = data.path("first_name").asText("");
        String lastName = data.path("last_name").asText("");
        String photoUrl = data.path("image_url").asText("");

        ProfileDTO updatedProfile = ProfileDTO.builder()
                .clerkId(clerkId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .photoUrl(photoUrl)
                .build();

        updatedProfile = profileService.updateProfile(updatedProfile);

        if(updatedProfile == null){
            handleUserCreate(data);
        }
    }

    private void handleUserCreate(JsonNode data) {
        String clerkId =  data.path("id").asText();

        String email ="";
        JsonNode emailAddresses = data.path("email_addresses");
        if(emailAddresses.isArray() && emailAddresses.size()>0){
            email = emailAddresses.get(0).path("email_address").asText();
        }

        String firstName = data.path("first_name").asText("");
        String lastName = data.path("last_name").asText("");
        String photoUrl = data.path("image_url").asText("");

        ProfileDTO newProfile = ProfileDTO.builder()
                .clerkId(clerkId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .photoUrl(photoUrl)
                .build();

        profileService.createProfile(newProfile);
        userCreditsService.createInitialCredits(clerkId);
    }

    private boolean verifyWebhookSignature(String svixId, String svixSignature, String svixTimestamp, String payload) {
        // Implement signature verification logic here
        return true; // Placeholder
    }
}