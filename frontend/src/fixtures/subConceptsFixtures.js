const subConceptsFixtures = {
  severalSubConcepts: [
    {
      id: 11,
      label: "Declaring variables",
      description: "Create a variable with a name and type.",
      example: 'String name = "Ada";',
      parentId: 1,
      parentLabel: "Variables",
      parent: {
        id: 1,
        label: "Variables",
      },
      sortOrder: 1,
    },
    {
      id: 12,
      label: "Updating variables",
      description: "Assign a new value to an existing variable.",
      example: "count = count + 1;",
      parentId: 1,
      parentLabel: "Variables",
      parent: {
        id: 1,
        label: "Variables",
      },
      sortOrder: 2,
    },
  ],
};

export default subConceptsFixtures;
