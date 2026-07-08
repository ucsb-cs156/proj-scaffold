import {
  createContext,
  useContext,
  useState,
  type ReactNode,
} from "react";
import { useCurrentUser, hasRole } from "main/utils/currentUser";

interface DebugModeContextType {
  debugMode: boolean;
  setDebugMode: (value: boolean) => void;
  canUseDebugMode: boolean;
}

const DebugModeContext = createContext<DebugModeContextType>({
  debugMode: false,
  setDebugMode: () => {},
  canUseDebugMode: false,
});

export function DebugModeProvider({ children }: { children: ReactNode }) {
  const currentUser = useCurrentUser();
  const canUseDebugMode =
    hasRole(currentUser, "ROLE_ADMIN") ||
    hasRole(currentUser, "ROLE_INSTRUCTOR");

  const [debugMode, setDebugModeState] = useState<boolean>(() => {
    try {
      return sessionStorage.getItem("debugMode") === "true";
    } catch {
      return false;
    }
  });

  const setDebugMode = (value: boolean) => {
    try {
      sessionStorage.setItem("debugMode", String(value));
    } catch {
      // sessionStorage not available
    }
    setDebugModeState(value);
  };

  return (
    <DebugModeContext.Provider
      value={{
        debugMode: canUseDebugMode && debugMode,
        setDebugMode,
        canUseDebugMode,
      }}
    >
      {children}
    </DebugModeContext.Provider>
  );
}

export function useDebugMode(): DebugModeContextType {
  return useContext(DebugModeContext);
}
