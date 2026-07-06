package edu.ucsb.cs.scaffold.enums;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
public class CourseOptionsTests {

  @Test
  public void test_InsertStatus() {
    assert (CourseOptions.TRANSLATE_SECTIONS.toString().equals("TRANSLATE_SECTIONS"));
    assert (CourseOptions.ENABLE_CANVAS.toString().equals("ENABLE_CANVAS"));
  }
}
