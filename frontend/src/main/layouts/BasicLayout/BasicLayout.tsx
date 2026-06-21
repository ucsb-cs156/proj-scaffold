import type { ReactNode } from "react";
import AppNavbar from "../../components/Nav/AppNavbar";
import Footer from "../../components/Nav/Footer";

interface BasicLayoutProps {
  children: ReactNode;
}

export default function BasicLayout({ children }: BasicLayoutProps) {
  return (
    <div
      style={{
        minHeight: "100svh",
        display: "flex",
        flexDirection: "column",
      }}
    >
      <AppNavbar />
      <main
        style={{
          width: "1126px",
          maxWidth: "100%",
          margin: "0 auto",
          borderInline: "1px solid var(--border)",
          background: "#f7ede1",
          flex: 1,
          minHeight: 0,
          display: "flex",
          flexDirection: "column",
        }}
      >
        {children}
      </main>
      <Footer />
    </div>
  );
}
