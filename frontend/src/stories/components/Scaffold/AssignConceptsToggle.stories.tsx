import type { Meta, StoryObj } from "@storybook/react";
import AssignConceptsToggle from "main/components/Scaffold/AssignConceptsToggle";

const meta: Meta<typeof AssignConceptsToggle> = {
  title: "components/Scaffold/AssignConceptsToggle",
  component: AssignConceptsToggle,
  tags: ["autodocs"],
};

export default meta;
type Story = StoryObj<typeof AssignConceptsToggle>;

export const Inactive: Story = {
  args: {
    active: false,
    disabled: false,
    onClick: () => {},
  },
};

export const Active: Story = {
  args: {
    active: true,
    disabled: false,
    onClick: () => {},
  },
};

export const Disabled: Story = {
  args: {
    active: false,
    disabled: true,
    onClick: () => {},
  },
};
