import React from "react";
import { http, HttpResponse } from "msw";
import ScaffoldTabComponent from "main/components/Courses/TabComponents/ScaffoldTabComponent";

export default {
  title: "components/Courses/TabComponents/ScaffoldTabComponent",
  component: ScaffoldTabComponent,
};

const Template = (args) => <ScaffoldTabComponent {...args} />;

export const Default = Template.bind({});
Default.args = {
  courseId: 1,
  testIdPrefix: "storybook",
};

Default.parameters = {
  msw: [
    http.post("/api/course/scaffold/reset", ({ request }) => {
      const url = new URL(request.url);
      window.alert(
        `Invoked scaffold reset with courseId=${url.searchParams.get("courseId")} via URL: ${url}`,
      );
      return HttpResponse.json(
        { message: "Scaffold reset completed." },
        { status: 200 },
      );
    }),
  ],
};
