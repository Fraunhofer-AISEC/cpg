import { SyntaxKind, SourceFile, Node, createProgram, forEachChild } from 'typescript';
import { parse as svelteParse } from "svelte/compiler";
// @ts-ignore: Deno-specific import
import { parseArgs } from "https://deno.land/std@0.218.2/cli/parse_args.ts";
import * as path from "node:path"; // Keep using node:path for consistency if needed elsewhere

// --- Argument Parsing ---
// @ts-ignore: Deno-specific API
const flags = parseArgs(Deno.args, {
    string: ["language"],
    default: { language: "typescript" },
});

const language = flags.language.toLowerCase();
const filePathArg = flags._[0];

if (!filePathArg) {
    console.error("Error: File path argument is missing.");
    // @ts-ignore: Deno-specific API
    Deno.exit(1);
}

// Resolve the file path similar to how it was done before
const file = path.resolve(filePathArg.toString()); // Ensure it's a string and resolve

// --- Main Logic ---
// Use an async IIFE (Immediately Invoked Function Expression) to allow top-level await
// while satisfying older TypeScript/module targets.
(async () => {
    try {
        // @ts-ignore: Deno-specific API
        const fileContent = await Deno.readTextFile(file);
        let ast: any; // To hold the final AST object

        if (language === "svelte") {
            // --- Svelte Parsing ---
            ast = svelteParse(fileContent, { filename: file });
            // Simple serialization for Svelte AST - might need refinement
            // based on what SvelteLanguageFrontend expects.
            // We can potentially add a `type` field at the root for consistency?
            // ast.type = "SvelteProgram"; // Example

        } else if (language === "typescript") {
            // --- TypeScript Parsing ---
            const program = createProgram([file], {
                allowJs: true,
            });

            const sf = program.getSourceFile(file);
            if (!sf) {
                throw new Error(`Could not get source file: ${file}`);
            }
            ast = buildTsAstObject(sf, sf); // Build the TS AST object

        } else {
            throw new Error(`Unsupported language: ${language}`);
        }

        // --- Output ---
        // Use JSON.stringify for consistent output
        // Note: JSON.stringify might fail on circular structures if they exist
        // in either AST. The simple TS structure shouldn't have them.
        // Svelte AST might need custom replacer if it has cycles or complex objects.
        console.log(JSON.stringify(ast, null, 0)); // Use 0 for compact output

    } catch (error) {
        console.error(`Error processing file ${file}:`, error);
        // @ts-ignore: Deno-specific API
        Deno.exit(1);
    }
})();

// --- TypeScript AST Building Function ---
// Refactored from printTree to return an object
function buildTsAstObject(sf: SourceFile, node: Node): any {
    const output: any = {
        type: SyntaxKind[node.kind],
        code: node.getText(sf),
        location: { file: file, pos: node.pos, end: node.end },
        children: [] // Initialize children array
    };

    // Use forEachChild to iterate over significant children
    forEachChild(node, child => {
        output.children.push(buildTsAstObject(sf, child));
    });

    // If no children were added, remove the empty array for cleaner output
    if (output.children.length === 0) {
        delete output.children;
    }

    return output;
}

/* // Original printTree - kept for reference
let indent = 0;
sources.filter(sf => sf.fileName.endsWith(file)).forEach(sf => {
    console.log(printTree(sf, sf, false));
})

function printTree(sf: SourceFile, node: Node, needsComma: boolean): string {
    var output = " ".repeat(indent) + `{ "type": "${SyntaxKind[node.kind]}"`

    //output += `, "code": "${node.getText(sf).replace(/"/g, "\"").replace(/\n/g, "\\n")}"`
    output += `, "code": ${JSON.stringify(node.getText(sf))}`

    indent++;

    // need to use forEachChild, otherwise, we will get additional syntax nodes, that we do not want
    var numChildren = 0;
    forEachChild(node, x => {
        numChildren++;
    })

    if (numChildren == 1) {
        output += `, "children": [`;
        forEachChild(node, x => {
            output += printTree(sf, x, false);
        });
        output += "]";
    } else if (numChildren > 0) {
        output += `, "children": [\n`;

        var i = 0;
        forEachChild(node, x => {
            //console.log(`${i} == ${numChildren}`)
            output += printTree(sf, x, i < numChildren - 1)
            i++;
        });

        output += " ".repeat(indent - 1) + "\n]";
    }

    output += `, "location": {"file": "${file}", "pos": ${node.pos}, "end": ${node.end}}`;

    output += " }";

    if (needsComma) {
        output += ",\n"
    }

    indent--;

    return output
}
*/
