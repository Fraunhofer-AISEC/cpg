import * as fs from 'fs';
import * as svelte from 'svelte/compiler';
import * as path from 'path';

// Get the file path from command line arguments
const filePath = process.argv[2];

if (!filePath) {
    console.error("Error: No file path provided.");
    process.exit(1);
}

const absoluteFilePath = path.resolve(filePath);

try {
    // Read the Svelte file content
    const fileContent = fs.readFileSync(absoluteFilePath, 'utf8');

    // Parse the content using svelte.parse
    const ast = svelte.parse(fileContent, {
        filename: absoluteFilePath, // Optional: provide filename for better error messages
        // Add other Svelte 5 parse options if needed, check Svelte documentation
    });

    // Stringify the AST and print to stdout
    // Using null, 2 for pretty printing, which might be large but good for debugging initially
    // For production, might remove indentation (null, 0)
    const astJson = JSON.stringify(ast, null, 0);
    process.stdout.write(astJson);

} catch (error: any) {
    // Output errors to stderr
    const errorOutput = {
        error: true,
        message: error.message,
        filename: absoluteFilePath,
        position: error.start ? { line: error.start.line, column: error.start.column } : null,
        stack: error.stack
    }
    console.error(JSON.stringify(errorOutput));
    process.exit(1);
} 