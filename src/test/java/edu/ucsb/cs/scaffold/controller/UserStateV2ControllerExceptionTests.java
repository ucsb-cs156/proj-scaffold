package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

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

    var request = new UserStateV2Controller.UserStateV2Request(1L, 2L, null, null, null, null);
    assertThrows(IllegalArgumentException.class, () -> controller.upsertUserState(request));
  }

  @Test
  void upsertUserStateRetriesOnceAfterAConcurrentDuplicateInsert() throws JsonProcessingException {
    UserStateV2 insertedByConcurrentRequest = new UserStateV2();
    // First lookup finds nothing (so the first attempt tries to insert); the retry's lookup
    // finds the row a concurrent request just inserted (so the retry updates it instead).
    when(repository.findByUseridAndCourseId(1L, 2L))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(insertedByConcurrentRequest));
    when(objectMapper.writeValueAsString(any())).thenReturn("[]");
    when(repository.save(any()))
        .thenThrow(new DataIntegrityViolationException("duplicate key"))
        .thenReturn(insertedByConcurrentRequest);

    var request = new UserStateV2Controller.UserStateV2Request(1L, 2L, null, null, null, null);
    ResponseEntity<Void> response = controller.upsertUserState(request);

    assertEquals(204, response.getStatusCode().value());
    verify(repository, times(2)).save(any());
  }

  @Test
  void upsertUserStateWritesAProvidedTopLevelPositionsPayload() throws JsonProcessingException {
    when(repository.findByUseridAndCourseId(1L, 2L)).thenReturn(Optional.empty());
    when(objectMapper.writeValueAsString(any())).thenReturn("[]");
    com.fasterxml.jackson.databind.node.ObjectNode positions =
        new ObjectMapper().createObjectNode();
    positions.putObject("recursion").put("x", 100).put("y", 200);
    // Distinguishable from the "[]" default so the saved state can only carry this value if
    // state.setTopLevelPositions(...) actually ran (not just that writeJson was called).
    when(objectMapper.writeValueAsString(positions)).thenReturn("{\"recursion\":{\"x\":100}}");

    var request = new UserStateV2Controller.UserStateV2Request(1L, 2L, null, null, null, positions);
    ResponseEntity<Void> response = controller.upsertUserState(request);

    assertEquals(204, response.getStatusCode().value());
    ArgumentCaptor<UserStateV2> captor = ArgumentCaptor.forClass(UserStateV2.class);
    verify(repository).save(captor.capture());
    assertEquals("{\"recursion\":{\"x\":100}}", captor.getValue().getTopLevelPositions());
  }
}
