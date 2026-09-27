import { useMemo, useState } from "react";
import { Form, Pagination } from "react-bootstrap";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import UsersTable, { type User } from "main/components/Users/UsersTable";
import { useBackend } from "main/utils/useBackend";

const pageSizeOptions = [10, 25, 50, 100, 500];

export default function AdminUsersPage(): React.JSX.Element {
  const { data: users } = useBackend<User[]>(
    ["/api/admin/users"],
    { method: "GET", url: "/api/admin/users" },
    [],
  );
  const [pageSize, setPageSize] = useState(10);
  const [pageIndex, setPageIndex] = useState(0);

  const pageCount = Math.max(1, Math.ceil((users?.length ?? 0) / pageSize));
  const currentPageIndex = Math.min(pageIndex, pageCount - 1);

  const pagedUsers = useMemo(() => {
    const start = currentPageIndex * pageSize;
    return (users ?? []).slice(start, start + pageSize);
  }, [currentPageIndex, pageSize, users]);

  return (
    <BasicLayout>
      <div className="pt-2">
        <h1>Users</h1>
        <div className="d-flex align-items-center gap-2 mb-3">
          <Form.Label className="mb-0" htmlFor="admin-users-page-size">
            Page Size
          </Form.Label>
          <Form.Select
            aria-label="Page Size"
            data-testid="AdminUsersPage-page-size"
            id="admin-users-page-size"
            onChange={(event) => {
              setPageSize(Number(event.target.value));
              setPageIndex(0);
            }}
            style={{ width: "auto" }}
            value={pageSize}
          >
            {pageSizeOptions.map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </Form.Select>
        </div>
        <UsersTable users={pagedUsers} />
        <Pagination className="mt-3">
          <Pagination.Prev
            data-testid="AdminUsersPage-prev"
            disabled={currentPageIndex === 0}
            onClick={() => setPageIndex(Math.max(currentPageIndex - 1, 0))}
          />
          {Array.from({ length: pageCount }, (_, index) => (
            <Pagination.Item
              active={index === currentPageIndex}
              data-testid={`AdminUsersPage-page-${index + 1}`}
              key={index + 1}
              onClick={() => setPageIndex(index)}
            >
              {index + 1}
            </Pagination.Item>
          ))}
          <Pagination.Next
            data-testid="AdminUsersPage-next"
            disabled={currentPageIndex >= pageCount - 1}
            onClick={() =>
              setPageIndex(Math.min(currentPageIndex + 1, pageCount - 1))
            }
          />
        </Pagination>
      </div>
    </BasicLayout>
  );
}
