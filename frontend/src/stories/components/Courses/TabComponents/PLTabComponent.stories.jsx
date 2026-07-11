import React from "react";
import { http, HttpResponse } from "msw";
import PLTabComponent from "main/components/Courses/TabComponent/PLTabComponent";

export default {
  title: "components/Courses/TabComponents/PLTabComponent",
  component: PLTabComponent,
};

const Template = (args) => <PLTabComponent {...args} />;

export const Default = Template.bind({});
Default.args = {
  courseId: 1,
  testIdPrefix: "storybook",
};
Default.parameters = {
  msw: [
    http.put("/api/courses/updateGithubRepo", () =>
      HttpResponse.json({ id: 1, plRepoId: 9 }),
    ),
  ],
};

export const MissingPat = Template.bind({});
MissingPat.args = {
  courseId: 1,
  testIdPrefix: "storybook",
};
MissingPat.parameters = {
  msw: [
    http.put("/api/courses/updateGithubRepo", () =>
      HttpResponse.json(
        { type: "ForbiddenException", message: "must set up Github PAT first" },
        { status: 403 },
      ),
    ),
  ],
};
