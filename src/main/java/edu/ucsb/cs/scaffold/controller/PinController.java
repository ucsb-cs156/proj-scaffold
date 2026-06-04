package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.model.PinRequest;
import edu.ucsb.cs.scaffold.repository.StudentPinRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Student PIN")
@RestController
@RequiredArgsConstructor
public class PinController {

    private final StudentPinRepository studentPinRepository;

    @Operation(summary = "Validate a student PIN – returns {\"valid\":true} if the PIN exists")
    @PostMapping("/api/validate-pin")
    public Map<String, Boolean> validatePin(@RequestBody PinRequest body) {
        String pin = body.getPin();
        boolean valid = !studentPinRepository.findByPin(pin).isEmpty();
        return Map.of("valid", valid);
    }
}
