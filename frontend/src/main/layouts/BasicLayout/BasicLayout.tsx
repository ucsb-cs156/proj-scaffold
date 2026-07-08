import { useEffect, type ReactNode } from "react";
import AppNavbar from "../../components/Nav/AppNavbar";
import Footer from "../../components/Nav/Footer";
import { useLogout } from "../../utils/currentUser";

interface BasicLayoutProps {
  children: ReactNode;
  // When true (the Scaffold Canvas pages), the viewport is pinned so the
  // React Flow canvas handles all pan/zoom itself: a class is added to
  // <body> that index.css uses to disable page scrolling. Every other page
  // scrolls normally.
  lockScroll?: boolean;
}

export default function BasicLayout({
  children,
  lockScroll = false,
}: BasicLayoutProps) {
  const doLogout = useLogout().mutate;

  useEffect(() => {
    if (!lockScroll) return;
    document.body.classList.add("scaffold-canvas-active");
    return () => document.body.classList.remove("scaffold-canvas-active");
  }, [lockScroll]);

  return (
    <div
      style={{
        minHeight: "100svh",
        display: "flex",
        flexDirection: "column",
      }}
    >
      <AppNavbar doLogout={doLogout} />
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
