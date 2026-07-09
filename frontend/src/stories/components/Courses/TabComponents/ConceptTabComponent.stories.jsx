import React from "react";
<<<<<<< HEAD
import { http, HttpResponse } from "msw";
=======
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
>>>>>>> origin/main
import ConceptTabComponent from "main/components/Courses/TabComponents/ConceptTabComponent";
import conceptsFixtures from "fixtures/conceptsFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/Courses/TabComponents/ConceptTabComponent",
  component: ConceptTabComponent,
};

const QueryWrapper = ({ children }) => {
  const queryClient = new QueryClient();
  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

const Template = (args) => (
  <QueryWrapper>
    <ConceptTabComponent {...args} />
  </QueryWrapper>
);

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
  testIdPrefix: "storybook",
};

Default.parameters = {
  msw: [
<<<<<<< HEAD
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
=======
    http.get("/api/concepts/course", () =>
      HttpResponse.json(conceptsFixtures.severalConcepts),
    ),
    http.post("/api/concept", async ({ request }) => {
      const body = await request.json();
      window.alert(
        "Invoked post on /api/concept with body: " + JSON.stringify(body),
      );
      return HttpResponse.json({ id: 3, ...body }, { status: 200 });
>>>>>>> origin/main
    }),
  ],
};
