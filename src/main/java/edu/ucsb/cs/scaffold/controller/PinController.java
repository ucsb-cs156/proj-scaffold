package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.repository.StudentPinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PinController {

    private final StudentPinRepository studentPinRepository;

    @PostMapping("/validate-pin")
    public Map<String, Boolean> validatePin(@RequestBody Map<String, String> body) {
        String pin = body.get("pin");
        boolean valid = !studentPinRepository.findByPin(pin).isEmpty();
        return Map.of("valid", valid);
    }
}
