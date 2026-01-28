package com.bhavani.resumeforge.service;

import com.bhavani.resumeforge.document.Resume;
import com.bhavani.resumeforge.dto.AuthResponse;
import com.bhavani.resumeforge.repository.ResumeRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {

    private final Cloudinary cloudinary;
    private final AuthService authService;
    private final ResumeRepository resumeRepository;

    public Map<String, String> uploadSingleImage(MultipartFile file) throws IOException {
        Map<String, Object> imageUploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "image"));
        log.info("Inside FileUploadService: uploadSingleImage() {}", imageUploadResult.get("secure_url").toString());
        return Map.of("imageUrl", imageUploadResult.get("secure_url").toString());
    }

    public Map<String, String> uploadResumeImages(String resumeId,
                                                  Object principal,
                                                  MultipartFile thumbnail,
                                                  MultipartFile profileImage) throws IOException {
        //Step 1: get the current profile
        AuthResponse response = authService.getProfile(principal);

        //Step 2: get the existing resume
        Resume existingResume = resumeRepository.findByUserIdAndId(response.getId(), resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        //Step 3: upload the resume images and set the resume
        Map<String, String> returnValue = new HashMap<>();
        Map<String, String> uploadResult;

        if(Objects.nonNull(thumbnail)) {
            uploadResult = uploadSingleImage(thumbnail);
            existingResume.setThumbnailLink(uploadResult.get("imageUrl"));
            returnValue.put("thumbnailLink", uploadResult.get("imageUrl"));
        }

        if(Objects.nonNull(profileImage)) {
            uploadResult = uploadSingleImage(profileImage);

            if (Objects.isNull(existingResume.getProfileInfo())) {
                existingResume.setProfileInfo(new Resume.ProfileInfo());
            }
            existingResume.getProfileInfo().setProfilePreviewUrl(uploadResult.get("imageUrl"));
            returnValue.put("profilePreviewUrl", uploadResult.get("imageUrl"));

        }
        //Step 4: update the details into db
        resumeRepository.save(existingResume);
        returnValue.put("message", "Images uploaded successfully");
        //Step 5: return the result
        return returnValue;
    }
}