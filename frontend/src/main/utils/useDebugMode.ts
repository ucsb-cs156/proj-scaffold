import { useContext } from "react";
import {
  DebugModeContext,
  type DebugModeContextType,
} from "main/utils/debugModeContext";

export function useDebugMode(): DebugModeContextType {
  return useContext(DebugModeContext);
}
