import { createContext } from "react";

export interface DebugModeContextType {
  debugMode: boolean;
  setDebugMode: (value: boolean) => void;
  canUseDebugMode: boolean;
}

export const DebugModeContext = createContext<DebugModeContextType>({
  debugMode: false,
  setDebugMode: () => {},
  canUseDebugMode: false,
});
