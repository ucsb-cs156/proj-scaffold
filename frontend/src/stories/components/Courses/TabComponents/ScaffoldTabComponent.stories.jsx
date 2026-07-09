import React from "react";
import ScaffoldTabComponent from "main/components/Courses/TabComponents/ScaffoldTabComponent";

export default {
  title: "components/Courses/TabComponents/ScaffoldTabComponent",
  component: ScaffoldTabComponent,
};

const Template = (args) => <ScaffoldTabComponent {...args} />;

export const Default = Template.bind({});
Default.args = {
  testIdPrefix: "storybook",
};
