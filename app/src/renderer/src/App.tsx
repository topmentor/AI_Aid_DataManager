import { useAppStore } from "./store/appStore";
import { StartScreen } from "./components/StartScreen";
import { ProjectWindow } from "./components/ProjectWindow";

export default function App() {
  const view = useAppStore((s) => s.view);
  return view === "start" ? <StartScreen /> : <ProjectWindow />;
}
