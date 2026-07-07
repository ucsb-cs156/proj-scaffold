package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.repository.UserActivityV2Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityV2ControllerExceptionTests {

  @Mock private UserActivityV2Repository repository;
  @Mock private ObjectMapper objectMapper;
  @InjectMocks private UserActivityV2Controller controller;

  @Test
  void writeJsonThrowsIllegalArgumentOnJsonProcessingException() throws JsonProcessingException {
    when(objectMapper.writeValueAsString(any()))
        .thenThrow(new JsonProcessingException("forced failure") {});

    var request = new UserActivityV2Controller.UserActivityV2Request(1L, 2L, "test", null);
    assertThrows(IllegalArgumentException.class, () -> controller.insertUserActivity(request));
  }
}
