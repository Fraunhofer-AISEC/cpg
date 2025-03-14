import { Route, HashRouter as Router, Routes } from "react-router-dom";
import Navbar from "./components/Navbar";
import ComponentPage from "./pages/ComponentPage";
import DashboardPage from "./pages/DashboardPage";
import TranslationUnitPage from "./pages/TranslationUnitPage";

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-gray-100">
        <Navbar />
        <main className="py-6">
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/component/:name" element={<ComponentPage />} />
            <Route path="/component/:componentName/translation-unit/*" element={<TranslationUnitPage />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
