import { SyntaxKind, SourceFile, Node, createProgram, forEachChild } from 'typescript';
import * as path from 'node:path';

const file = path.normalize(process.argv[2]);

const program = createProgram([file], {
    allowJs: true,
});

var sources = program.getSourceFiles()
let indent = 0;

sources.filter(sf => sf.fileName.endsWith(file)).forEach(sf => {
    console.log(printTree(sf, sf, false));
})

function printTree(sf: SourceFile, node: Node, needsComma: boolean): string {
    var output = " ".repeat(indent) + `{ "type": "${SyntaxKind[node.kind]}"`

    //output += `, "code": "${node.getText(sf).replace(/"/g, "\\\"").replace(/\n/g, "\\n")}"`
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
