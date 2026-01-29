package in.indra.cloudshareapi.service;

import in.indra.cloudshareapi.document.ProfileDocument;
import in.indra.cloudshareapi.dto.ProfileDTO;
import in.indra.cloudshareapi.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
// this class manage the user profile-related operations like create, update, delete and fetch profile
public class ProfileService {

    private final ProfileRepository profileRepository;

    // method to create a new user profile
    public ProfileDTO createProfile(ProfileDTO profileDTO) {
        if (profileRepository.existsByClerkId(profileDTO.getClerkId()))
            return updateProfile(profileDTO);

        ProfileDocument profile = ProfileDocument.builder()   // builder is used to create obj
                // DTO - Document (from API -- mongodb)
                .clerkId(profileDTO.getClerkId())
                .email(profileDTO.getEmail())
                .firstName(profileDTO.getFirstName())
                .lastName(profileDTO.getLastName())
                .credits(5)
                .photoUrl(profileDTO.getPhotoUrl())
                .createdAt(Instant.now())
                .build(); // finish the obj creation

        profile = profileRepository.save(profile);

        return ProfileDTO.builder()  // Returning response (Document → DTO)
                .id(profile.getId()) // it is coming from database for the response
                .clerkId(profile.getClerkId())
                .email(profile.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .photoUrl(profile.getPhotoUrl())
                .credits(profile.getCredits())
                .createdAt(profile.getCreatedAt())
                .build();
    }

    public ProfileDTO updateProfile(ProfileDTO profileDTO) {
        ProfileDocument existingProfile = profileRepository.findByClerkId(profileDTO.getClerkId());
        if(existingProfile != null){
            // update fields if provided
            if(profileDTO.getEmail()!= null && !profileDTO.getEmail().isEmpty()){
                existingProfile.setEmail(profileDTO.getEmail());
            }
            if(profileDTO.getFirstName() != null && !profileDTO.getFirstName().isEmpty()) {
                existingProfile.setFirstName(profileDTO.getFirstName());
            }
            if(profileDTO.getLastName() != null && !profileDTO.getLastName().isEmpty()) {
                existingProfile.setLastName(profileDTO.getLastName());
            }
            if(profileDTO.getPhotoUrl() != null && !profileDTO.getPhotoUrl().isEmpty()) {
                existingProfile.setPhotoUrl(profileDTO.getPhotoUrl());
            }
            profileRepository.save(existingProfile);

            return ProfileDTO.builder()
                    .id(existingProfile.getId())
                    .clerkId(existingProfile.getClerkId())
                    .email(existingProfile.getEmail())
                    .firstName(existingProfile.getFirstName())
                    .lastName(existingProfile.getLastName())
                    .photoUrl(existingProfile.getPhotoUrl())
                    .credits(existingProfile.getCredits())
                    .createdAt(existingProfile.getCreatedAt())
                    .build();
        }
        return null;
    }

    public boolean existsByClerkId(String clerkId) {
        return profileRepository.existsByClerkId(clerkId);
    }

    public void deleteProfile(String clerkId) {
        ProfileDocument profile = profileRepository.findByClerkId(clerkId);
        if (profile != null) {
            profileRepository.delete(profile);
        }
    }

    public ProfileDocument getCurrentProfile(){
        if(SecurityContextHolder.getContext().getAuthentication()== null){
            throw new UsernameNotFoundException("User not authenticated..");
        }
        String clerkId = SecurityContextHolder.getContext().getAuthentication().getName();
        return  profileRepository.findByClerkId(clerkId);
    }

    // Ensure profile exists or create a new one
    public ProfileDocument ensureProfileExists(ProfileDTO dto) {
        ProfileDocument profile =
                profileRepository.findByClerkId(dto.getClerkId());
        if (profile != null) {
            return profile;
        }
        // Create profile if not exists
        ProfileDocument newProfile = new ProfileDocument();
        newProfile.setClerkId(dto.getClerkId());
        newProfile.setEmail(dto.getEmail());
        newProfile.setFirstName(dto.getFirstName());
        newProfile.setLastName(dto.getLastName());
        newProfile.setPhotoUrl(dto.getPhotoUrl());
        newProfile.setCredits(0);
        newProfile.setCreatedAt(Instant.now());

        return profileRepository.save(newProfile);
    }

}
