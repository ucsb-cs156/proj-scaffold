import React from "react";
import { http, HttpResponse } from "msw";
import ScaffoldTabComponent from "main/components/Courses/TabComponent/ScaffoldTabComponent";

export default {
  title: "components/Courses/TabComponents/ScaffoldTabComponent",
  component: ScaffoldTabComponent,
};

const Template = (args) => <ScaffoldTabComponent {...args} />;

const sampleYaml = `# Concept graph for course 1 (Storybook 101)
# See docs/yaml-format.md for the format.
format: 1
concepts:
  - id: 1
    label: Recursion
edges: []
`;

export const Default = Template.bind({});
Default.args = {
  courseId: 1,
  courseName: "Storybook 101",
  term: "F26",
  school: { key: "UCSB", displayName: "UCSB" },
  testIdPrefix: "storybook",
};

Default.parameters = {
  msw: [
    http.get("/api/courses/:courseId", ({ params }) => {
      return HttpResponse.json(
        { id: Number(params.courseId), xSpacing: 350, ySpacing: 300 },
        { status: 200 },
      );
    }),
    http.put("/api/course/scaffold/spacing", ({ request }) => {
      const url = new URL(request.url);
      const xSpacing = Number(url.searchParams.get("xSpacing"));
      const ySpacing = Number(url.searchParams.get("ySpacing"));
      window.alert(
        `Invoked scaffold spacing update with courseId=${url.searchParams.get("courseId")}, xSpacing=${xSpacing}, ySpacing=${ySpacing}`,
      );
      return HttpResponse.json({ xSpacing, ySpacing }, { status: 200 });
    }),
    http.get("/api/concepts/yaml/download", () => {
      return new HttpResponse(sampleYaml, {
        status: 200,
        headers: { "Content-Type": "application/x-yaml" },
      });
    }),
    http.post("/api/concepts/yaml/upload", ({ request }) => {
      const url = new URL(request.url);
      window.alert(
        `Invoked concepts YAML upload with courseId=${url.searchParams.get("courseId")} via URL: ${url}`,
      );
      return HttpResponse.json(
        {
          success: true,
          errors: [],
          conceptsCreated: 1,
          subconceptsCreated: 0,
          edgesCreated: 0,
          practiceProblemsCreated: 0,
          userStatesCleared: 0,
        },
        { status: 200 },
      );
    }),
  ],
};
