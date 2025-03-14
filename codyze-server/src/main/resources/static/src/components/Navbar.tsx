// src/main/web/src/components/Navbar.tsx
import React from 'react';
import { Link } from 'react-router-dom';

const Navbar: React.FC = () => {
    return (
        <nav className="bg-blue-600 text-white shadow-md">
            <div className="container mx-auto px-4 py-3 flex items-center justify-between">
<<<<<<< HEAD
                <Link to="/" className="text-xl font-bold">CPG Web Console</Link>
=======
                <Link to="/" className="text-xl font-bold">Codyze Console</Link>
>>>>>>> origin/webconsole
                <div>
                    <Link to="/" className="px-3 py-2 hover:bg-blue-700 rounded">Dashboard</Link>
                </div>
            </div>
        </nav>
    );
};

export default Navbar;