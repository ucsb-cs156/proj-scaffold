import type { Meta, StoryObj } from "@storybook/react";
import ConceptGraph from "main/components/Scaffold/ConceptGraph";

const meta: Meta<typeof ConceptGraph> = {
  title: "components/Scaffold/ConceptGraph",
  component: ConceptGraph,
  parameters: {
    layout: "fullscreen",
  },
  tags: ["autodocs"],
};

export default meta;
type Story = StoryObj<typeof ConceptGraph>;

export const Default: Story = {
  args: {
    highlightedIds: new Set<string>(),
    highlightedSubconcepts: new Map<string, Set<string>>(),
    onConceptClick: () => {},
    starredIds: new Set<string>(),
    onStarClick: () => {},
    onReset: () => {},
    masteredSubconcepts: new Set<string>(),
    onSubconceptMastered: () => {},
  },
};
