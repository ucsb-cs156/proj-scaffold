package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.model.UserIdRequest;
import edu.ucsb.cs.scaffold.repository.StudentUserIdRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Student User ID")
@RestController
@RequiredArgsConstructor
public class UserIdController {

    private final StudentUserIdRepository studentUserIdRepository;

    @Operation(summary = "Validate a student user ID; returns {\"valid\":true} if the user ID exists")
    @PostMapping("/api/validate-userid")
    public Map<String, Boolean> validateUserId(@RequestBody UserIdRequest body) {
        String userid = body.getUserid();
        boolean valid = !studentUserIdRepository.findByUserid(userid).isEmpty();
        return Map.of("valid", valid);
    }
}
