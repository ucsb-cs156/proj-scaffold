package edu.ucsb.cs.scaffold.entity;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class CourseOptionTests {

  @Test
  public void test_noArgsConstructor() {
    CourseOption option = new CourseOption();
    assertNotNull(option);
  }
}
