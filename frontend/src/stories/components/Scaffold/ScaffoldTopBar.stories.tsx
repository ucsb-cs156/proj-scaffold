import type { Meta, StoryObj } from "@storybook/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import ScaffoldTopBar from "main/components/Scaffold/ScaffoldTopBar";
import { StaffToolsProvider } from "main/utils/staffTools";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import type { Assessment, Course, Question } from "main/types/conceptGraph";

const sampleAssessments: Assessment[] = [
  { id: "1", pl_assessment_id: "pl1", name: "HW1" },
  { id: "2", pl_assessment_id: "pl2", name: "HW2" },
  { id: "3", pl_assessment_id: "pl3", name: "Scaffold Final" },
];

const sampleQuestions: Question[] = [
  {
    id: "q1",
    assessment_id: "1",
    pl_question_uuid: "u1",
    title: "Loops 101",
  },
  {
    id: "q2",
    assessment_id: "1",
    pl_question_uuid: "u2",
    title: "Recursion basics",
  },
];

const sampleCourse: Course = { id: 1, courseName: "CMPSC 156" };

const meta: Meta<typeof ScaffoldTopBar> = {
  title: "components/Scaffold/ScaffoldTopBar",
  component: ScaffoldTopBar,
  tags: ["autodocs"],
  args: {
    course: sampleCourse,
    courseId: 1,
    assessments: sampleAssessments,
    selectedAssessmentId: "",
    onSelectAssessment: () => {},
    questions: [],
    selectedQuestionId: "",
    onSelectQuestion: () => {},
    numStarredConcepts: 3,
    numTotalConcepts: 12,
  },
};

export default meta;
type Story = StoryObj<typeof ScaffoldTopBar>;

export const Default: Story = {};

export const AssessmentSelected: Story = {
  args: {
    selectedAssessmentId: "1",
    questions: sampleQuestions,
  },
};

// Shows the "Unlock Assessments" button, which only renders for an
// admin/instructor. StaffToolsProvider derives canUseStaffTools from the
// current-user query, so it's preset here the same way AdminDeveloperPage's
// story presets systemInfo.
export const AsInstructor: Story = {
  decorators: [
    (Story) => {
      const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false } },
      });
      queryClient.setQueryData(["current user"], currentUserFixtures.adminUser);
      return (
        <QueryClientProvider client={queryClient}>
          <StaffToolsProvider>
            <Story />
          </StaffToolsProvider>
        </QueryClientProvider>
      );
    },
  ],
};
