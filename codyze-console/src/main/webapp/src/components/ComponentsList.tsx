import React from 'react';
import { Link } from 'react-router-dom';
import { ComponentJSON } from '../types';

interface ComponentsListProps {
    components: ComponentJSON[];
}

const ComponentsList: React.FC<ComponentsListProps> = ({ components }) => {
    return (
        <>
            <h3 className="text-lg font-medium mb-2">Components</h3>
            <ul className="divide-y divide-gray-200">
                {components.map((component) => (
                    <li key={component.name} className="py-3">
                        <Link
                            to={`/component/${component.name}`}
                            className="text-blue-600 hover:underline text-lg font-medium"
                        >
                            {component.name}
                        </Link>
                        <p className="text-gray-600 text-sm mt-1">
                            {component.translationUnits.length} translation units
                        </p>
                    </li>
                ))}
            </ul>
        </>
    );
};

export default ComponentsList;