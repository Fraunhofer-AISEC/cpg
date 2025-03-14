// src/main/web/src/pages/ComponentPage.tsx
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getComponent } from "../services/api";
import { ComponentJSON } from "@/types";

function ComponentPage() {
  const { name } = useParams<{ name: string }>();
  const [component, setComponent] = useState<ComponentJSON | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchComponent = async () => {
      if (!name) {
        setError("Component name is missing");
        setLoading(false);
        return;
      }

      try {
        const data = await getComponent(name);
        setComponent(data);
      } catch (err) {
        setError("Failed to load component data");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchComponent();
  }, [name]);

  if (loading) {
    return (
      <div className="container mx-auto p-4">Loading component data...</div>
    );
  }

  if (error || !component) {
    return (
      <div className="container mx-auto p-4">
        <div className="rounded bg-red-100 p-3 text-red-700">
          {error || "Component not found"}
        </div>
        <div className="mt-4">
          <Link to="/" className="text-blue-600 hover:underline">
            Back to Dashboard
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4">
      <div className="mb-4">
        <Link to="/" className="text-blue-600 hover:underline">
          Back to Dashboard
        </Link>
      </div>

      <h1 className="mb-6 text-2xl font-bold">{component.name}</h1>
      <p className="mb-4 text-gray-500">{component.topLevel}</p>

      <div className="rounded bg-white p-6 shadow-md">
        <h2 className="mb-4 text-xl font-semibold">Translation Units</h2>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                  Name
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white">
              {component.translationUnits.map((unit) => (
                <tr key={unit.path}>
                  <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-gray-900">
                    {unit.name}
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-500">
                    <Link
                      to={`/translation-unit?component=${component.name}&path=${encodeURIComponent(unit.path)}`}
                      className="text-blue-600 hover:underline"
                    >
                      View Details
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default ComponentPage;
