package edu.ucsb.cs156.example.controllers;

import edu.ucsb.cs156.example.entities.Account;
import edu.ucsb.cs156.example.repositories.AccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/csvupload")
public class CSVUploadController extends ApiController {

  @Autowired AccountRepository accountRepository;

  @PostMapping(value = "/accounts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Upload accounts from CSV file")
  @ApiResponse(
      responseCode = "200",
      description = "Accounts uploaded successfully",
      content = @Content(mediaType = "application/json"))
  @ApiResponse(responseCode = "400", description = "Bad request")
  public ResponseEntity<Object> uploadAccountsFromCsv(
      @Parameter(
              description = "CSV file containing email and pin columns",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                      schema = @Schema(type = "string", format = "binary")))
          @RequestPart("file")
          MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("message", "CSV file is required"));
    }

    List<Account> accounts = new ArrayList<>();

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
          continue;
        }

        String[] parts = trimmed.split(",", -1);
        if (parts.length < 2) {
          continue;
        }

        String email = parts[0].trim();
        String pin = parts[1].trim();

        if (email.equalsIgnoreCase("email") && pin.equalsIgnoreCase("pin")) {
          continue;
        }

        if (email.isEmpty() || pin.isEmpty()) {
          continue;
        }

        Account account = new Account();
        account.setEmail(email);
        account.setPin(pin);
        accounts.add(account);
      }

      accountRepository.saveAll(accounts);

      Map<String, Object> response = new HashMap<>();
      response.put("created", accounts.size());
      response.put("message", "Accounts uploaded successfully");
      return ResponseEntity.ok(response);
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "Unable to read CSV file"));
    }
  }
}
