package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.model.LegacyUserState;
import edu.ucsb.cs.scaffold.repository.LegacyUserStateRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LegacyUserStateControllerExceptionTests {

  @Mock private LegacyUserStateRepository repository;
  @Mock private ObjectMapper objectMapper;
  @InjectMocks private LegacyUserStateController controller;

  @Test
  void parseStringListThrowsIllegalStateOnJsonProcessingException() throws JsonProcessingException {
    LegacyUserState state = new LegacyUserState();
    when(repository.findByUserid(1L)).thenReturn(Optional.of(state));
    when(objectMapper.readValue(anyString(), any(TypeReference.class)))
        .thenThrow(new JsonProcessingException("forced failure") {});

    assertThrows(IllegalStateException.class, () -> controller.getUserState(1L));
  }

  @Test
  void parseJsonNodeThrowsIllegalStateOnJsonProcessingException() throws JsonProcessingException {
    LegacyUserState state = new LegacyUserState();
    when(repository.findByUserid(1L)).thenReturn(Optional.of(state));
    when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of());
    when(objectMapper.readTree(anyString()))
        .thenThrow(new JsonProcessingException("forced failure") {});

    assertThrows(IllegalStateException.class, () -> controller.getUserState(1L));
  }

  @Test
  void writeJsonThrowsIllegalArgumentOnJsonProcessingException() throws JsonProcessingException {
    when(repository.findByUserid(any())).thenReturn(Optional.empty());
    when(objectMapper.writeValueAsString(any()))
        .thenThrow(new JsonProcessingException("forced failure") {});

    var request = new LegacyUserStateController.LegacyUserStateRequest(1L, null, null, null);
    assertThrows(IllegalArgumentException.class, () -> controller.upsertUserState(request));
  }
}
