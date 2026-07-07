package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.model.UserStateV2;
import edu.ucsb.cs.scaffold.repository.UserStateV2Repository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class UserStateV2ControllerExceptionTests {

  @Mock private UserStateV2Repository repository;
  @Mock private ObjectMapper objectMapper;
  @InjectMocks private UserStateV2Controller controller;

  @Test
  void parseStringListThrowsIllegalStateOnJsonProcessingException() throws JsonProcessingException {
    UserStateV2 state = new UserStateV2();
    when(repository.findByUseridAndCourseId(1L, 2L)).thenReturn(Optional.of(state));
    when(objectMapper.readValue(anyString(), any(TypeReference.class)))
        .thenThrow(new JsonProcessingException("forced failure") {});

    assertThrows(IllegalStateException.class, () -> controller.getUserState(1L, 2L));
  }

  @Test
  void parseJsonNodeThrowsIllegalStateOnJsonProcessingException() throws JsonProcessingException {
    UserStateV2 state = new UserStateV2();
    when(repository.findByUseridAndCourseId(1L, 2L)).thenReturn(Optional.of(state));
    when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of());
    when(objectMapper.readTree(anyString()))
        .thenThrow(new JsonProcessingException("forced failure") {});

    assertThrows(IllegalStateException.class, () -> controller.getUserState(1L, 2L));
  }

  @Test
  void writeJsonThrowsIllegalArgumentOnJsonProcessingException() throws JsonProcessingException {
    when(repository.findByUseridAndCourseId(any(), any())).thenReturn(Optional.empty());
    when(objectMapper.writeValueAsString(any()))
        .thenThrow(new JsonProcessingException("forced failure") {});

    var request = new UserStateV2Controller.UserStateV2Request(1L, 2L, null, null, null);
    assertThrows(IllegalArgumentException.class, () -> controller.upsertUserState(request));
  }
}
