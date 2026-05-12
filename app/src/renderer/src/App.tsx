import { useAppStore } from "./store/appStore";
import { StartScreen } from "./components/StartScreen";

// Temporary placeholder — will be replaced in Task 11
function ProjectWindow() {
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        height: "100vh",
        color: "#888",
      }}
    >
      메인 화면 (Tasks 10-12에서 구현)
    </div>
  );
}

export default function App() {
  const view = useAppStore((s) => s.view);
  return view === "start" ? <StartScreen /> : <ProjectWindow />;
}
