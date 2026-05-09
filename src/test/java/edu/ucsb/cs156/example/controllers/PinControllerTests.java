package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.Account;
import edu.ucsb.cs156.example.repositories.AccountRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = PinController.class)
@Import(TestConfig.class)
public class PinControllerTests extends ControllerTestCase {

  @MockitoBean AccountRepository accountRepository;
  @MockitoBean UserRepository userRepository;

  @Test
  public void getCurrentUsersPin__logged_out() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/user/pin")).andReturn();
    assertEquals(403, result.getResponse().getStatus());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void getCurrentUsersPin__logged_in_account_exists() throws Exception {
    // The mock user's email is "user@example.org" (see MockCurrentUserServiceImpl)
    Account account = Account.builder().email("user@example.org").pin("1234").build();
    when(accountRepository.findById("user@example.org")).thenReturn(Optional.of(account));

    MvcResult result = mockMvc.perform(get("/api/user/pin")).andReturn();

    String responseString = result.getResponse().getContentAsString();
    assertEquals(200, result.getResponse().getStatus());
    assertEquals("1234", responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void getCurrentUsersPin__logged_in_no_account() throws Exception {
    when(accountRepository.findById("user@example.org")).thenReturn(Optional.empty());

    Exception exception =
        assertThrows(Exception.class, () -> mockMvc.perform(get("/api/user/pin")).andReturn());

    assertTrue(exception.getMessage().contains("No value present"));
    assertTrue(exception.getCause() instanceof NoSuchElementException);
  }
}
