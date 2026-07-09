import OurTable, { ButtonColumn } from "main/components/Common/OurTable";

export default function SubConceptTable({
  subConcepts = [],
  editCallback = () => {},
  deleteCallback = () => {},
  testId = "SubConceptTable",
}) {
  const columns = [
    {
      header: "id",
      accessorKey: "id",
      id: "id",
    },
    {
      header: "label",
      accessorKey: "label",
      id: "label",
    },
    {
      header: "parentId",
      accessorKey: "parent.id",
      id: "parentId",
    },
    {
      header: "parentLabel",
      accessorKey: "parent.label",
      id: "parentLabel",
    },
    ButtonColumn("Edit", "primary", editCallback, testId),
    ButtonColumn("Delete", "danger", deleteCallback, testId),
  ];

  return <OurTable data={subConcepts} columns={columns} testid={testId} />;
}
