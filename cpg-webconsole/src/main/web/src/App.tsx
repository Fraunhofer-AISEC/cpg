import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Dashboard from './pages/Dashboard';
import ComponentPage from './pages/ComponentPage';
import TranslationUnitPage from './pages/TranslationUnitPage';

const App: React.FC = () => {
    return (
        <Router>
            <div className="min-h-screen bg-gray-100">
                <Navbar />
                <main className="py-6">
                    <Routes>
                        <Route path="/" element={<Dashboard />} />
                        <Route path="/component/:name" element={<ComponentPage />} />
                        <Route path="/translation-unit" element={<TranslationUnitPage />} />
                    </Routes>
                </main>
            </div>
        </Router>
    );
};

export default App;