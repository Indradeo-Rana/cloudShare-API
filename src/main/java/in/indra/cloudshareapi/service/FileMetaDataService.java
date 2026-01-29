package in.indra.cloudshareapi.service;

import in.indra.cloudshareapi.document.FileMetaDataDocument;
import in.indra.cloudshareapi.document.ProfileDocument;
import in.indra.cloudshareapi.dto.FileMetaDataDTO;
import in.indra.cloudshareapi.repository.FileMetaDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileMetaDataService {

    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;
    private final FileMetaDataRepository fileMetaDataRepository;

    public List<FileMetaDataDTO> uploadFiles(MultipartFile[] files) throws IOException {

        ProfileDocument currentProfile = profileService.getCurrentProfile();
        List<FileMetaDataDocument> savedFiles = new ArrayList<>();

        if (!userCreditsService.hasEnoughCredits(files.length)) {
            throw new RuntimeException("Not enough credits to upload files. Please upgrade your plan.");
        }

        Path uploadPath = Paths.get("uploads").toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        for (MultipartFile file : files) {
            String fileName = UUID.randomUUID() + "." +
                    StringUtils.getFilenameExtension(file.getOriginalFilename());

            Path targetLocation = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            FileMetaDataDocument fileMetaData = FileMetaDataDocument.builder()
                    .fileLocation(targetLocation.toString())
                    .name(file.getOriginalFilename())
                    .size(file.getSize())
                    .type(file.getContentType())
                    .clerkId(currentProfile.getClerkId())
                    .isPublic(false)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            // consume one credit for each file upload
            userCreditsService.consumeCredits();

            savedFiles.add(fileMetaDataRepository.save(fileMetaData));
        }

        return savedFiles.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private FileMetaDataDTO mapToDTO(FileMetaDataDocument fileMetaDataDocument) {

        return FileMetaDataDTO.builder()
                .id(fileMetaDataDocument.getId())
                .fileLocation(fileMetaDataDocument.getFileLocation())
                .name(fileMetaDataDocument.getName())
                .size(fileMetaDataDocument.getSize())
                .type(fileMetaDataDocument.getType())
                .clerkId(fileMetaDataDocument.getClerkId())
                .isPublic(fileMetaDataDocument.getIsPublic())
                .uploadedAt(fileMetaDataDocument.getUploadedAt())
                .build();
    }

    public List<FileMetaDataDTO> getFiles(){
        ProfileDocument currentProfile = profileService.getCurrentProfile();
        List<FileMetaDataDocument> files = fileMetaDataRepository.findByClerkId(currentProfile.getClerkId());
        return files.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public FileMetaDataDTO getPublicFile(String id){
        Optional<FileMetaDataDocument> fileOptional = fileMetaDataRepository.findById(id);
        if(fileOptional.isEmpty() || !fileOptional.get().getIsPublic()){
            throw new RuntimeException("Unable to get the file due to it's not public or doesn't exist.");
        }
        FileMetaDataDocument document = fileOptional.get();
        return mapToDTO(document);
    }

    public FileMetaDataDTO getDownloadableFile(String id){
        FileMetaDataDocument file = fileMetaDataRepository.findById(id).orElseThrow(() -> new RuntimeException("File not found"));
        return mapToDTO(file);
    }

//    public void deleteFile(String id) {
//        try {
//            ProfileDocument currentProfile = profileService.getCurrentProfile();
//            FileMetaDataDocument file = fileMetaDataRepository.findById(id)
//                    .orElseThrow(() -> new RuntimeException("file not found"));
//            if (file.getClerkId().equals(currentProfile.getClerkId())) {
//                throw new RuntimeException(("File is not belong to current user"));
//            }
//            Path filePath = Paths.get(file.getFileLocation());
//            Files.deleteIfExists(filePath);
//
//            fileMetaDataRepository.deleteById(id);
//        } catch (Exception e) {
//            throw new RuntimeException("Error deleting file: " + e.getMessage());
//        }
//    }

    //
    public void deleteFile(String id) {
        try {
            ProfileDocument currentProfile = profileService.getCurrentProfile();
            FileMetaDataDocument file = fileMetaDataRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("file not found"));
            if (!file.getClerkId().equals(currentProfile.getClerkId())) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                        "File does not belong to current user");
            }
            Path filePath = Paths.get(file.getFileLocation());
            Files.deleteIfExists(filePath);

            fileMetaDataRepository.deleteById(id);
        } catch (org.springframework.web.server.ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error deleting file: " + e.getMessage());
        }
    }
    //

    public FileMetaDataDTO togglePublic (String id){
      FileMetaDataDocument file = fileMetaDataRepository.findById(id)
              .orElseThrow(() -> new RuntimeException("file not found"));
      file.setIsPublic(!file.getIsPublic());
      file = fileMetaDataRepository.save(file);
      return mapToDTO(file);
    }
}