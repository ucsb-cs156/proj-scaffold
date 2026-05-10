package edu.ucsb.cs156.example.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.example.entities.Account;
import edu.ucsb.cs156.example.models.CurrentUser;
import edu.ucsb.cs156.example.repositories.AccountRepository;
import edu.ucsb.cs156.example.services.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is a REST controller for getting information about the users.
 *
 * <p>These endpoints are only accessible to users with the role "ROLE_ADMIN".
 */
@Tag(name = "PIN information (user only)")
@RequestMapping("/api/user/pin")
@RestController
public class PinController extends ApiController {
  @Autowired AccountRepository accountRepository;

  @Autowired CurrentUserService currentUserService;

  @Autowired ObjectMapper mapper;

  /**
   * This method returns a list of all users. Accessible only to users with the role "ROLE_ADMIN".
   *
   * @return a list of all users
   * @throws JsonProcessingException if there is an error processing the JSON
   */
  @Operation(summary = "Get current user's PIN")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("")
  public String getCurrentUsersPin() {
    CurrentUser currentUser = currentUserService.getCurrentUser();
    String email = currentUser.getUser().getEmail();
    Optional<Account> account = accountRepository.findById(email);
    if (account.isEmpty()) {
      return "xxxx";
    }

    return account.get().getPin();
  }
}
