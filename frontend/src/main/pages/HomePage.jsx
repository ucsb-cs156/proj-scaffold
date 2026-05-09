import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { useSystemInfo } from "main/utils/systemInfo";
import { useBackend } from "main/utils/useBackend";

import Card from "react-bootstrap/Card";

export default function HomePage() {
  const systemInfo = useSystemInfo();

  // Stryker disable all

  var oauthLogin = systemInfo?.oauthLogin || "/oauth2/authorization/google";

  const scaffold = "https://ucsbscaffold.vercel.app/";

  const { data: pin } = useBackend(
    [`/api/user/pin`],
    {
      method: "GET",
      url: "/api/user/pin",
    },
    null,
    true, // suppressToasts
  );

  // Stryker restore all

  return (
    <BasicLayout>
      <Card>
        <Card.Body>
          <Card.Title className="display-3">Welcome to Scaffold!</Card.Title>
          <Card.Text className="fs-1">
            {pin === null ? (
              <p className="fs-1">
                To view your pin, please
                <a href={oauthLogin} className="btn btn-primary ms-2 ">
                  Login
                </a>
              </p>
            ) : (
              <>
                <p className="fs-1"> Your pin for Scaffold is: {pin}.</p>
                <p>
                  To proceed to Scaffold, follow the link below, and type in
                  your pin:
                </p>
                <p>
                  <a href={scaffold}>{scaffold}</a>
                </p>
              </>
            )}
          </Card.Text>
        </Card.Body>
      </Card>
    </BasicLayout>
  );
}
