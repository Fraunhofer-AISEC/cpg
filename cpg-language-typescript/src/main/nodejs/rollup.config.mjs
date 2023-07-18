import typescript from "@rollup/plugin-typescript";
import resolve from "@rollup/plugin-node-resolve";
import commonjs from "@rollup/plugin-commonjs";

export default {
  input: "src/parser.ts",
  output: {
    dir: "../../../build/resources/main/nodejs",
    format: "commonjs",
  },
  plugins: [
    commonjs({
      include: ["./src/parser.js", "node_modules/**"],
    }),
    typescript(), resolve({
      typescript: true,
      path: true
    })],
};
