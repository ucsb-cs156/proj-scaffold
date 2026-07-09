import React from "react";

export default function ConceptTabComponent({ testIdPrefix }) {
  return (
    <div className="tabComponent" data-testid={`${testIdPrefix}-conceptTab`}>
      <h2>Concepts</h2>
    </div>
  );
}
