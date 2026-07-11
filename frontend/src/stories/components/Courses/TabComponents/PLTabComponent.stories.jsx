import React from "react";
import PLTabComponent from "main/components/Courses/TabComponent/PLTabComponent";

export default {
  title: "components/Courses/TabComponents/PLTabComponent",
  component: PLTabComponent,
};

const Template = (args) => <PLTabComponent {...args} />;

export const Default = Template.bind({});
Default.args = {
  testIdPrefix: "storybook",
};
