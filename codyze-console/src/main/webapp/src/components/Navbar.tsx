// src/main/web/src/components/Navbar.tsx
import React from "react";
import { Link } from "react-router-dom";

const Navbar: React.FC = () => {
  return (
    <nav className="bg-blue-600 text-white shadow-md">
      <div className="container mx-auto flex items-center justify-between px-4 py-3">
        <Link to="/" className="text-xl font-bold">
          Codyze Console
        </Link>
        <div>
          <Link to="/" className="rounded px-3 py-2 hover:bg-blue-700">
            Dashboard
          </Link>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
