package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.repository.LegacyUserActivityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LegacyUserActivityControllerExceptionTests {

  @Mock private LegacyUserActivityRepository repository;
  @Mock private ObjectMapper objectMapper;
  @InjectMocks private LegacyUserActivityController controller;

  @Test
  void writeJsonThrowsIllegalArgumentOnJsonProcessingException() throws JsonProcessingException {
    when(objectMapper.writeValueAsString(any()))
        .thenThrow(new JsonProcessingException("forced failure") {});

    var request = new LegacyUserActivityController.LegacyUserActivityRequest(1L, "test", null);
    assertThrows(IllegalArgumentException.class, () -> controller.insertUserActivity(request));
  }
}
