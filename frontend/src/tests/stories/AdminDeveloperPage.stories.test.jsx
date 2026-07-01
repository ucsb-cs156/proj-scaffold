import meta, {
  Default,
} from "../../stories/pages/Admin/AdminDeveloperPage.stories";

describe("AdminDeveloperPage story", () => {
  test("has expected story metadata", () => {
    expect(meta.title).toBe("pages/Admin/AdminDeveloperPage");
    expect(Default).toBeDefined();
  });
});
