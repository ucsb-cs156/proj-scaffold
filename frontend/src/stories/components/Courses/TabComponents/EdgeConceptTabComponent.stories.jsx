import React from "react";
import EdgeConceptTabComponent from "main/components/Courses/TabComponents/EdgeConceptTabComponent";

export default {
  title: "components/Courses/TabComponents/EdgeConceptTabComponent",
  component: EdgeConceptTabComponent,
};

const Template = (args) => <EdgeConceptTabComponent {...args} />;

export const Default = Template.bind({});
Default.args = {
  testIdPrefix: "storybook",
};
