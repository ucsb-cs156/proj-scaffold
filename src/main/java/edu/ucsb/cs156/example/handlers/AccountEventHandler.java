package edu.ucsb.cs156.example.handlers;

import edu.ucsb.cs156.example.entities.Account;
import java.util.regex.Pattern;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
public class AccountEventHandler {

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

  @HandleBeforeCreate
  @HandleBeforeSave
  public void validate(Account account) {

    // Custom business logic validation

    if (account.getPin() == null || account.getPin().length() != 4) {
      throw new IllegalArgumentException("PIN must be exactly 4 characters long.");
    }

    if (account.getEmail() == null || !EMAIL_PATTERN.matcher(account.getEmail()).matches()) {
      throw new IllegalArgumentException("Email format is invalid.");
    }
  }
}
