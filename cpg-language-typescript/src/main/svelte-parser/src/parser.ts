// Deno script to parse a Svelte file and output JSON AST
// We need to figure out the correct way to import svelte/compiler in Deno
// and handle potential Node.js built-in compatibility needs.

// Example using npm specifier (needs testing)
import * as svelte from "npm:svelte@^4.0.0/compiler"; // Use a recent svelte version
import { readFileSync } from 'node:fs'; // Deno std/node compatibility
import * as path from 'node:path'; // Deno std/node compatibility

if (Deno.args.length < 1) {
  console.error("Usage: deno run --allow-read <script.ts> <file_to_parse.svelte>");
  Deno.exit(1);
}

const filePath = path.normalize(Deno.args[0]);

try {
  const fileContent = readFileSync(filePath, { encoding: 'utf-8' });

  // Parse the Svelte file
  const ast = svelte.parse(fileContent, {
    filename: filePath // Optional: provide filename for better error messages
    // Add other parser options if needed
  });

  // Output the AST as JSON string to stdout
  console.log(JSON.stringify(ast, null, 2)); // Pretty print for readability

} catch (error) {
  console.error(`Error parsing Svelte file ${filePath}:`, error);
  // Output error details as JSON to stderr for potential capture by frontend
  let errorOutput = { 
    message: error.message || "Unknown parsing error",
    position: error.position ? { line: error.line, column: error.column } : null,
    filename: error.filename || filePath,
    fullError: error.toString()
  };
  console.error(JSON.stringify(errorOutput));
  Deno.exit(1);
} 