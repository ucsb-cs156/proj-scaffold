import React from "react";
import SubConceptTabComponent from "main/components/Courses/TabComponents/SubConceptTabComponent";

export default {
  title: "components/Courses/TabComponents/SubConceptTabComponent",
  component: SubConceptTabComponent,
};

const Template = (args) => <SubConceptTabComponent {...args} />;

export const Default = Template.bind({});
Default.args = {
  testIdPrefix: "storybook",
};
