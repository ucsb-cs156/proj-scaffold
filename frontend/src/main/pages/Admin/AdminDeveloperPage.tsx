import { useSystemInfo } from "../../utils/systemInfo";

export default function AdminDeveloperPage() {
  const { data: systemInfo } = useSystemInfo();

  return (
    <main style={{ padding: "24px" }}>
      <h1>Developer Information</h1>
      <h2>Current Deployed Branch</h2>
      <table>
        <tbody>
          <tr>
            <td>GitHub Repo:</td>
            <td>
              {systemInfo?.sourceRepo ? (
                <a href={systemInfo.sourceRepo}>{systemInfo.sourceRepo}</a>
              ) : (
                "Not available"
              )}
            </td>
          </tr>
        </tbody>
      </table>

      <h2>Backend Endpoints</h2>
      <ul>
        {systemInfo?.showSwaggerUILink && (
          <li>
            <a href="/swagger-ui/index.html">Swagger</a>
          </li>
        )}
        {systemInfo?.springH2ConsoleEnabled && (
          <li>
            <a href="/h2-console">H2 Console</a>
          </li>
        )}
      </ul>

      <h2>System Info</h2>
      <pre>{JSON.stringify(systemInfo, null, 2)}</pre>
    </main>
  );
}
