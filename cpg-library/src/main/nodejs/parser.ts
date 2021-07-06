import * as ts from 'typescript';

const file = process.argv[2];
const program = ts.createProgram([file], {});
var sourceFile = program.getSourceFile(file)

let indent = 0;

console.log(printTree(sourceFile, false));

function printTree(node: ts.Node, needsComma: boolean): string {
    var output = " ".repeat(indent) + `{ "type": "${ts.SyntaxKind[node.kind]}"`

    indent++;

    // need to use forEachChild, otherwise, we will get additional syntax nodes, that we do not want
    var numChildren = 0;
    ts.forEachChild(node, x => {
        numChildren++;
    })

    if (numChildren == 1) {
        output += `, "children": [`;
        ts.forEachChild(node, x => {
            output += printTree(x, false);
        });
        output += "]";
    } else if (numChildren > 0) {
        output += `, "children": [\n`;

        var i = 0;
        ts.forEachChild(node, x => {
            //console.log(`${i} == ${numChildren}`)
            output += printTree(x, i < numChildren - 1)
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