package com.bhavani.resumeforge.service;

import com.bhavani.resumeforge.document.Resume;
import com.bhavani.resumeforge.dto.AuthResponse;
import com.bhavani.resumeforge.dto.CreateResumeRequest;
import com.bhavani.resumeforge.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final AuthService authService;

    public Resume createResume(CreateResumeRequest request, Object principalObject) {
        //Step 1: Create the Resume object
        Resume newResume = new Resume();

        //Step 2: get the current profile
        AuthResponse response = authService.getProfile(principalObject);

        //Step 3: update the resume object
        newResume.setUserId(response.getId());
        newResume.setTitle(request.getTitle());

        //Step 4: set default data for resume
        setDefaultResumeData(newResume);

        //Step 5: save the resume data
        return resumeRepository.save(newResume);
    }

    private void setDefaultResumeData(Resume newResume) {
        newResume.setProfileInfo(new Resume.ProfileInfo());
        newResume.setContactInfo(new Resume.ContactInfo());
        newResume.setWorkExperiences(new ArrayList<>());
        newResume.setEducations(new ArrayList<>());
        newResume.setSkills(new ArrayList<>());
        newResume.setProjects(new ArrayList<>());
        newResume.setCertifications(new ArrayList<>());
        newResume.setLanguages(new ArrayList<>());
        newResume.setInterests(new ArrayList<>());
    }

    public List<Resume> getUserResumes( Object principal) {
        //Step 1: Get the current profile
        AuthResponse response = authService.getProfile(principal);
        //step 2: Call the repository finder method
        List<Resume> resumes = resumeRepository.findByUserIdOrderByUpdatedAtDesc(response.getId());
        //step 3: return response
        return resumes;
    }

    public Resume getResumeById(String resumeId, Object principal) {
        //Step 1: get the current profile
        AuthResponse response = authService.getProfile(principal);
        //Step 2: Call the repo finder method
        Resume existingResume = resumeRepository.findByUserIdAndId(response.getId(), resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
        //Step 3: return the result
        return existingResume;
    }

    public Resume updateResume(String resumeId, Resume updatedData, Object principal) {
        //Step 1: get the current profile
        AuthResponse response = authService.getProfile(principal);
        //Step 2: call the repository finder method
        Resume existingResume = resumeRepository.findByUserIdAndId(response.getId(), resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        //Step 3: update the new data
        existingResume.setTitle(updatedData.getTitle());
        existingResume.setThumbnailLink(updatedData.getThumbnailLink());
        existingResume.setTemplate(updatedData.getTemplate());
        existingResume.setProfileInfo(updatedData.getProfileInfo());
        existingResume.setContactInfo(updatedData.getContactInfo());
        existingResume.setWorkExperiences(updatedData.getWorkExperiences());
        existingResume.setEducations(updatedData.getEducations());
        existingResume.setSkills(updatedData.getSkills());
        existingResume.setProjects(updatedData.getProjects());
        existingResume.setCertifications(updatedData.getCertifications());
        existingResume.setLanguages(updatedData.getLanguages());
        existingResume.setInterests(updatedData.getInterests());

        //Step 4: update the details into db
        resumeRepository.save(existingResume);
        //Step 5: return the result
        return existingResume;
    }

    public void deleteResume(String resumeId, Object principal) {
        //Step 1: get the current profile
        AuthResponse response = authService.getProfile(principal);
        //Step 2: call the repository finder method
        Resume existingResume = resumeRepository.findByUserIdAndId(response.getId(), resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
        resumeRepository.delete(existingResume);
    }
}