import React from "react";

export default function ScaffoldTabComponent({ testIdPrefix }) {
  return (
    <div className="tabComponent" data-testid={`${testIdPrefix}-scaffoldTab`}>
      <h2>Scaffold</h2>
    </div>
  );
}
