/**
 * Utility functions for formatting and display
 */

/**
 * Shortens a fully qualified class name by returning only the class name
 * e.g., "com.example.package.MyClass" becomes "MyClass"
 * @param className - The fully qualified class name
 * @param methodName - The method name
 * @returns Formatted string with just the class name and method
 */
export function getShortCallerInfo(className: string, methodName: string): string {
  const parts = className.split('.');
  // Get only the last part (just the class name)
  const shortName = parts[parts.length - 1];
  return `${shortName}.${methodName}()`;
}

/**
 * Shortens a file path to show only the filename
 * @param filePath - The full file path
 * @returns Just the filename
 */
export function getShortFileName(filePath: string): string {
  return filePath.split('/').pop() || filePath;
}
