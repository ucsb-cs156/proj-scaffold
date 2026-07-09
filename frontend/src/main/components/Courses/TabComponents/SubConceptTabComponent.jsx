import React from "react";

export default function SubConceptTabComponent({ testIdPrefix }) {
  return (
    <div className="tabComponent" data-testid={`${testIdPrefix}-subConceptTab`}>
      <h2>SubConcepts</h2>
    </div>
  );
}
