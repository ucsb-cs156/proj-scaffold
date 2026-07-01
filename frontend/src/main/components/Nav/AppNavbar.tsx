import { Link } from "react-router";
import { hasRole, useCurrentUser } from "main/utils/currentUser";
import { useSystemInfo } from "main/utils/systemInfo";
import { Container, Nav, Navbar, NavDropdown } from "react-bootstrap";
import GoogleLogin from "main/components/Nav/GoogleLogin";

export default function AppNavbar({
  doLogout,
}: {
  doLogout: () => void;
}): React.JSX.Element {
  const currentUser = useCurrentUser();
  const { data: systemInfo } = useSystemInfo();

  const handleLogin = () => {
    window.location.href =
      systemInfo?.oauthLogin ?? "/oauth2/authorization/google";
  };

  return (
    <Navbar expand="md" sticky="top" data-testid="AppNavbar">
      <Container>
        <Navbar.Brand as={Link} to="/">
          Scaffold
        </Navbar.Brand>

        <Navbar.Toggle />

        <>
          {/* be sure that each NavDropdown has a unique id and data-testid  */}
        </>

        <Navbar.Collapse className="justify-content-between">
          <Nav className="mr-auto">
            {systemInfo?.showSwaggerUILink && (
              <>
                <Nav.Link href="/swagger-ui/index.html">Swagger</Nav.Link>
              </>
            )}
            {systemInfo?.springH2ConsoleEnabled && (
              <>
                <Nav.Link href="/h2-console">H2Console</Nav.Link>
              </>
            )}
            {hasRole(currentUser, "ROLE_ADMIN") && (
              <NavDropdown
                title="Admin"
                id="appnavbar-admin-dropdown"
                data-testid="appnavbar-admin-dropdown"
              >
                <NavDropdown.Item as={Link} to="/admin/admins">
                  Admins
                </NavDropdown.Item>
                <NavDropdown.Item as={Link} to="/admin/instructors">
                  Instructors
                </NavDropdown.Item>
                <NavDropdown.Item as={Link} to="/admin/developer">
                  Developer Info
                </NavDropdown.Item>
              </NavDropdown>
            )}
          </Nav>
          <Nav className="ml-auto">
            <GoogleLogin
              currentUser={currentUser}
              handleLogin={handleLogin}
              doLogout={doLogout}
            />
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}
