package edu.ucsb.cs156.example.handlers;

import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.ucsb.cs156.example.entities.Account;
import org.junit.jupiter.api.Test;

public class AccountEventHandlerTests {
  @Test
  public void checksEmail() {
    Account account = Account.builder().email("badEmail").pin("1234").build();
    assertThrows(
        IllegalArgumentException.class, () -> (new AccountEventHandler()).validate(account));
  }

  @Test
  public void checksNullEmail() {
    Account account = Account.builder().email(null).pin("1234").build();
    assertThrows(
        IllegalArgumentException.class, () -> (new AccountEventHandler()).validate(account));
  }

  @Test
  public void checksPin() {
    Account account = Account.builder().email("test@example.com").pin("123").build();
    assertThrows(
        IllegalArgumentException.class, () -> (new AccountEventHandler()).validate(account));
  }

  @Test
  public void checksNullPin() {
    Account account = Account.builder().email("test@example.com").pin(null).build();
    assertThrows(
        IllegalArgumentException.class, () -> (new AccountEventHandler()).validate(account));
  }

  @Test
  void validAccount() {
    AccountEventHandler handler = new AccountEventHandler();
    handler.validate(Account.builder().email("test@example.com").pin("1234").build());
    handler.validate(Account.builder().email("user@domain.com").pin("5678").build());
    handler.validate(Account.builder().email("admin@company.org").pin("9012").build());
  }
}
