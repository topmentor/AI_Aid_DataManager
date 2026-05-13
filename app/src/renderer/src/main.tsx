import React from "react";
import ReactDOM from "react-dom/client";
import * as monaco from "monaco-editor";
import { loader } from "@monaco-editor/react";
import App from "./App";
import "./styles/global.css";

// Use local monaco-editor package instead of CDN (required in Electron)
(window as unknown as Record<string, unknown>).MonacoEnvironment = {
  getWorker(_: unknown, _label: string) {
    const blob = new Blob(["self.onmessage=function(){}"], { type: "application/javascript" });
    return new Worker(URL.createObjectURL(blob));
  },
};
loader.config({ monaco });

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
