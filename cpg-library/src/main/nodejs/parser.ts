import * as ts from 'typescript';

const file = process.argv[2];



const program = ts.createProgram([file], {
    allowJs: true,
});

var sources = program.getSourceFiles()

let indent = 0;

sources.filter(sf => sf.fileName.endsWith(file)).forEach(sf => {
    console.log(printTree(sf, sf, false));
})

function printTree(sf: ts.SourceFile, node: ts.Node, needsComma: boolean): string {
    var output = " ".repeat(indent) + `{ "type": "${ts.SyntaxKind[node.kind]}"`

    //output += `, "code": "${node.getText(sf).replace(/"/g, "\\\"").replace(/\n/g, "\\n")}"`
    output += `, "code": ${JSON.stringify(node.getText(sf))}`

    indent++;

    // need to use forEachChild, otherwise, we will get additional syntax nodes, that we do not want
    var numChildren = 0;
    ts.forEachChild(node, x => {
        numChildren++;
    })

    if (numChildren == 1) {
        output += `, "children": [`;
        ts.forEachChild(node, x => {
            output += printTree(sf, x, false);
        });
        output += "]";
    } else if (numChildren > 0) {
        output += `, "children": [\n`;

        var i = 0;
        ts.forEachChild(node, x => {
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