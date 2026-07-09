import React from "react";
import ConceptTabComponent from "main/components/Courses/TabComponents/ConceptTabComponent";

export default {
  title: "components/Courses/TabComponents/ConceptTabComponent",
  component: ConceptTabComponent,
};

const Template = (args) => <ConceptTabComponent {...args} />;

export const Default = Template.bind({});
Default.args = {
  testIdPrefix: "storybook",
};
