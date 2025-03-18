export function getFindingStyle(kind: string | null): {
  backgroundColor: string;
  borderColor: string;
  color: string;
} {
  switch (kind?.toLowerCase()) {
    case 'fail':
    case 'error':
      return {
        backgroundColor: 'rgba(254, 226, 226, 1)', // bg-red-100
        borderColor: 'rgba(239, 68, 68, 1)', // border-red-500
        color: 'rgba(185, 28, 28, 1)' // text-red-700
      };
    case 'pass':
    case 'success':
      return {
        backgroundColor: 'rgba(220, 252, 231, 1)', // bg-green-100
        borderColor: 'rgba(34, 197, 94, 1)', // border-green-500
        color: 'rgba(21, 128, 61, 1)' // text-green-700
      };
    default:
      return {
        backgroundColor: 'rgba(243, 244, 246, 1)', // bg-gray-100
        borderColor: 'rgba(107, 114, 128, 1)', // border-gray-500
        color: 'rgba(55, 65, 81, 1)' // text-gray-700
      };
  }
}

export function getColorForNodeType(type: string): string {
  const colorMap: Record<string, string> = {
    FunctionDeclaration: 'rgba(255, 99, 132, 0.3)', // Red
    VariableDeclaration: 'rgba(54, 162, 235, 0.3)', // Blue
    RecordDeclaration: 'rgba(255, 206, 86, 0.3)', // Yellow
    Statement: 'rgba(75, 192, 192, 0.3)', // Green
    Expression: 'rgba(153, 102, 255, 0.3)', // Purple
    Literal: 'rgba(255, 159, 64, 0.3)', // Orange
    SetFileFlags: 'rgba(54, 162, 235, 0.3)', // Dark Blue for file operations
    SetFileMask: 'rgba(54, 162, 235, 0.3)', // Dark Blue for file operations
    CloseFile: 'rgba(54, 162, 235, 0.3)', // Dark Blue for file operations
    DeleteFile: 'rgba(54, 162, 235, 0.3)', // Dark Blue for file operations
    OpenFile: 'rgba(54, 162, 235, 0.3)', // Dark Blue for file operations
    ReadFile: 'rgba(54, 162, 235, 0.3)', // Dark Blue for file operations
    WriteFile: 'rgba(54, 162, 235, 0.3)', // Dark Blue for file operations
    Configuration: 'rgba(0, 0, 255, 0.3)', // Bright Blue for configuration
    ConfigurationGroup: 'rgba(0, 0, 255, 0.3)', // Bright Blue for configuration
    ConfigurationOption: 'rgba(0, 0, 255, 0.3)', // Bright Blue for configuration
    LoadConfiguration: 'rgba(0, 0, 255, 0.3)', // Bright Blue for configuration
    ReadConfigurationGroup: 'rgba(0, 0, 255, 0.3)', // Bright Blue for configuration
    ReadConfigurationOption: 'rgba(0, 0, 255, 0.3)', // Bright Blue for configuration
    RegisterConfigurationGroup: 'rgba(0, 0, 255, 0.3)', // Bright Blue for configuration
    RegisterConfigurationOption: 'rgba(0, 0, 255, 0.3)', // Bright Blue for configuration
    ProvideConfiguration: 'rgba(0, 0, 255, 0.3)', // Bright Blue for configuration
    ProvideConfigurationGroup: 'rgba(0, 0, 255, 0.3)', // Bright Blue for configuration
    ProvideConfigurationOption: 'rgba(0, 0, 255, 0.3)' // Bright Blue for configuration
  };

  return colorMap[type] || 'rgba(128, 128, 128, 0.3)';
}
