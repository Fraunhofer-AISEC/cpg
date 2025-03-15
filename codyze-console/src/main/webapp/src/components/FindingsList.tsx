import { Link } from "react-router-dom";
import { FindingsJSON } from "@/types";

interface FindingsListProps {
  findings: FindingsJSON[];
}

/**
 * Component to display a list of findings.
 * 
 * @param findings List of findings to display
 */
function FindingsList({ findings }: FindingsListProps) {
  return (
    <>
      <h3 className="mb-2 text-lg font-medium">Findings</h3>
      <ul className="divide-y divide-gray-200">
        {findings.map((finding, index) => (
          <li key={index} className="py-3">
            <p className="text-gray-700">
              <span className="font-medium">Kind:</span> {finding.kind}
            </p>
            <p className="text-gray-700">
              <span className="font-medium">Path: </span>
              <Link
                to={`/translation-unit?component=${finding.component}&path=${encodeURIComponent("file:" + finding.path)}&line=${finding.startLine}&findingText=${getText(finding)}&kind=${finding.kind.toLowerCase()}`}
                className="text-blue-600 hover:underline"
              >
                {finding.path}
              </Link>
            </p>
            <p className="text-gray-700">
              <span className="font-medium">Rule:</span> {finding.rule}
            </p>
            <p className="text-gray-700">
              <span className="font-medium">Location:</span> {finding.startLine}
              :{finding.startColumn} - {finding.endLine}:{finding.endColumn}
            </p>
          </li>
        ))}
      </ul>
    </>
  );
}

function getText(finding: FindingsJSON): string {
  return `${finding.kind}: ${finding.rule}`;
}

export default FindingsList;
