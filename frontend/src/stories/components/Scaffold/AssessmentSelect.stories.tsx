import type { Meta, StoryObj } from "@storybook/react";
import AssessmentSelect from "main/components/Scaffold/AssessmentSelect";
import type { Assessment } from "main/types/conceptGraph";

const sampleAssessments: Assessment[] = [
  {
    id: "1",
    pl_assessment_id: "pl1",
    name: "Homework 1",
    pl_assessment_set_abbreviation: "HW",
    pl_assessment_number: "1",
    pl_assessment_set_color: "#3B82F6",
  },
  {
    id: "2",
    pl_assessment_id: "pl2",
    name: "Homework 2",
    pl_assessment_set_abbreviation: "HW",
    pl_assessment_number: "2",
    pl_assessment_set_color: "#3B82F6",
  },
  {
    id: "3",
    pl_assessment_id: "pl3",
    name: "Scaffold Final",
    pl_assessment_set_abbreviation: "E",
    pl_assessment_number: "1",
    pl_assessment_set_color: "#EF4444",
  },
];

const meta: Meta<typeof AssessmentSelect> = {
  title: "components/Scaffold/AssessmentSelect",
  component: AssessmentSelect,
  tags: ["autodocs"],
  args: {
    assessments: sampleAssessments,
    selectedAssessmentId: "",
    onSelect: () => {},
  },
};

export default meta;
type Story = StoryObj<typeof AssessmentSelect>;

export const Default: Story = {};

export const Selected: Story = {
  args: {
    selectedAssessmentId: "2",
  },
};
