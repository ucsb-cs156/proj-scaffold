import React from "react";

export default function PLTabComponent({ testIdPrefix }) {
  return (
    <div className="tabComponent" data-testid={`${testIdPrefix}-plTab`}>
      <h2>PrairieLearn</h2>
    </div>
  );
}
