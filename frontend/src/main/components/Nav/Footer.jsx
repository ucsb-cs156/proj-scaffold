import { Container } from "react-bootstrap";
import { useSystemInfo } from "main/utils/systemInfo";

export default function Footer() {
  return (
    <footer className="bg-light pt-3 pt-md-4 pb-4 pb-md-5" data-testid="Footer">
      <Container>
        <p>
          Scaffold is a product of Kate Larrick at UC Santa Barbara.
        </p>
      </Container>
    </footer>
  );
}
