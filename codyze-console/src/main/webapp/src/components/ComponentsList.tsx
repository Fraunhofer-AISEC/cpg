import { Link } from "react-router-dom";
import { ComponentJSON } from "@/types";

interface ComponentsListProps {
  components: ComponentJSON[];
}

/**
 * Component to display a list of components.
 * 
 * @param components List of components to display
 */
function ComponentsList({ components }: ComponentsListProps) {
  return (
    <>
      <h3 className="mb-2 text-lg font-medium">Components</h3>
      <ul className="divide-y divide-gray-200">
        {components.map((component) => (
          <li key={component.name} className="py-3">
            <Link
              to={`/component/${component.name}`}
              className="text-lg font-medium text-blue-600 hover:underline"
            >
              {component.name}
            </Link>
            <p className="mt-1 text-sm text-gray-600">
              {component.translationUnits.length} translation units
            </p>
          </li>
        ))}
      </ul>
    </>
  );
}

export default ComponentsList;
