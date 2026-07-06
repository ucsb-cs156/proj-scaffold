package edu.ucsb.cs.scaffold.enums;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
public class InsertStatusTests {

  @Test
  public void test_InsertStatus() {
    assert (InsertStatus.INSERTED.toString().equals("INSERTED"));
    assert (InsertStatus.UPDATED.toString().equals("UPDATED"));
    assert (InsertStatus.REJECTED.toString().equals("REJECTED"));
  }
}
