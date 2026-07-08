package edu.ucsb.cs.scaffold.model;

import edu.ucsb.cs.scaffold.entity.RosterStudent;
import edu.ucsb.cs.scaffold.enums.InsertStatus;

public record UpsertResponse(InsertStatus insertStatus, RosterStudent rosterStudent) {
  public InsertStatus getInsertStatus() {
    return insertStatus;
  }
}
