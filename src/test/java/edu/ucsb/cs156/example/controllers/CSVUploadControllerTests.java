package edu.ucsb.cs156.example.controllers;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.Account;
import edu.ucsb.cs156.example.repositories.AccountRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(controllers = CSVUploadController.class)
@Import(TestConfig.class)
public class CSVUploadControllerTests extends ControllerTestCase {

  @Autowired CSVUploadController csvUploadController;

  @MockitoBean AccountRepository accountRepository;
  @MockitoBean UserRepository userRepository;

  @Test
  public void uploadAccountsFromCsv_empty_file_returns_bad_request() throws Exception {
    MockMultipartFile emptyCsv =
        new MockMultipartFile("file", "accounts.csv", "text/csv", new byte[0]);

    mockMvc
        .perform(multipart("/api/csvupload/accounts").file(emptyCsv).with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", is("CSV file is required")));

    verify(accountRepository, never()).saveAll(any());
  }

  @Test
  public void uploadAccountsFromCsv_null_file_returns_bad_request() {
    ResponseEntity<Object> response = csvUploadController.uploadAccountsFromCsv(null);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(Map.of("message", "CSV file is required"), response.getBody());
    verify(accountRepository, never()).saveAll(any());
  }

  @Test
  public void uploadAccountsFromCsv_valid_csv_saves_expected_accounts() throws Exception {
    AtomicReference<List<Account>> savedAccountsRef = new AtomicReference<>(new ArrayList<>());
    when(accountRepository.saveAll(any()))
        .thenAnswer(
            invocation -> {
              Iterable<Account> iterable = invocation.getArgument(0);
              List<Account> captured = new ArrayList<>();
              iterable.forEach(captured::add);
              savedAccountsRef.set(captured);
              return captured;
            });

    String csvContent =
        String.join(
            "\n",
            "email,pin",
            "   ",
            "email,9999",
            "alice@example.org,1234",
            "badline",
            " ,9999",
            "bob@example.org,5678",
            "charlie@example.org, ",
            "");

    MockMultipartFile csvFile =
        new MockMultipartFile("file", "accounts.csv", "text/csv", csvContent.getBytes());

    mockMvc
        .perform(multipart("/api/csvupload/accounts").file(csvFile).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created", is(3)))
        .andExpect(jsonPath("$.message", is("Accounts uploaded successfully")));

    verify(accountRepository).saveAll(any());

    List<Account> savedAccounts = savedAccountsRef.get();

    assertEquals(3, savedAccounts.size());
    assertEquals("email", savedAccounts.get(0).getEmail());
    assertEquals("9999", savedAccounts.get(0).getPin());
    assertEquals("alice@example.org", savedAccounts.get(1).getEmail());
    assertEquals("1234", savedAccounts.get(1).getPin());
    assertEquals("bob@example.org", savedAccounts.get(2).getEmail());
    assertEquals("5678", savedAccounts.get(2).getPin());
  }

  @Test
  public void uploadAccountsFromCsv_when_read_fails_returns_bad_request() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getInputStream()).thenThrow(new IOException("boom"));

    ResponseEntity<Object> response = csvUploadController.uploadAccountsFromCsv(file);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(Map.of("message", "Unable to read CSV file"), response.getBody());
    verify(accountRepository, never()).saveAll(any());
  }
}
