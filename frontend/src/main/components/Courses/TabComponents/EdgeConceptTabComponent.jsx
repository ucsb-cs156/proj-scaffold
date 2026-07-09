import React from "react";

export default function EdgeConceptTabComponent({ testIdPrefix }) {
  return (
    <div className="tabComponent" data-testid={`${testIdPrefix}-edgeConceptTab`}>
      <h2>Edges</h2>
    </div>
  );
}
