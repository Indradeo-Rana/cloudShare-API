package in.indra.cloudshareapi.service;

import in.indra.cloudshareapi.document.UserCredits;
import in.indra.cloudshareapi.repository.UserCreditsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCreditsService {

    private final UserCreditsRepository userCreditsRepository;
    private final ProfileService profileService;

    // Create initial credits for new user
    public UserCredits createInitialCredits(String clerkId) {

        return userCreditsRepository.findByClerkId(clerkId)
                .orElseGet(() -> {
                    UserCredits userCredits = UserCredits.builder()
                            .clerkId(clerkId)
                            .credits(5)
                            .plan("BASIC")
                            .build();
                    return userCreditsRepository.save(userCredits);
                });
    }

    // Get credits using clerkId
    public UserCredits getUserCredits(String clerkId) {
        return userCreditsRepository.findByClerkId(clerkId)
                .orElseGet(() -> createInitialCredits(clerkId));
    }

    // Get credits for currently logged-in user
    public UserCredits getUserCredits() {
        String clerkId = profileService.getCurrentProfile().getClerkId();
        return getUserCredits(clerkId);
    }

    // Check if user has enough credits
    public Boolean hasEnoughCredits(int requiredCredits) {
        UserCredits userCredits = getUserCredits();
        return userCredits.getCredits() >= requiredCredits;
    }

    // Consume one credit
    public UserCredits consumeCredits() {

        UserCredits userCredits = getUserCredits();

        if (userCredits.getCredits() <= 0) {
            return null;
        }

        userCredits.setCredits(userCredits.getCredits() - 1);
        return userCreditsRepository.save(userCredits);
    }

    // Add credits (after payment or upgrade)
    public UserCredits addCredits(String clerkId, Integer creditsToAdd, String plan) {

        UserCredits userCredits = userCreditsRepository.findByClerkId(clerkId)
                .orElseGet(() -> createInitialCredits(clerkId));

        int credits = creditsToAdd != null ? creditsToAdd : 0;
        userCredits.setCredits(userCredits.getCredits() + credits);
        userCredits.setPlan(plan);

        return userCreditsRepository.save(userCredits);
    }
}
