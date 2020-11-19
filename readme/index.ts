const Mustache = require('mustache');
const fs = require('fs');
const file = '../README.md.mustache';
const outputFile = '../README.md';
import { Octokit } from "@octokit/rest";

async function generate() {
    const octokit = new Octokit();

    let response = await octokit.repos.listReleases({ owner: "Fraunhofer-AISEC", repo: "cpg" });
    let version = response.data[0].name;

    fs.readFile(file, (err, data) => {
        if (err) {
            throw err;
        }

        const output = Mustache.render(data.toString(), { "version": version });
        fs.writeFileSync(outputFile, output);
    });
}

generate();