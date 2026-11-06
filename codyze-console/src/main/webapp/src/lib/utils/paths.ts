import type { ComponentJSON, TranslationUnitJSON } from '$lib/types';

/** A path with forward slashes and without the `file:` scheme. */
function clean(path: string): string {
  let result = path.replace(/\\/g, '/');
  if (result.startsWith('file:')) result = result.slice(5);
  return result.replace(/^\/{2,}/, '/');
}

const isUnder = (path: string, directory: string) => path.startsWith(`${directory}/`);

/** The longest directory that contains all the paths, or an empty string if there is none. */
function commonDirectory(paths: string[]): string {
  if (paths.length === 0) return '';
  const directories = paths.map((p) => p.split('/').slice(0, -1));
  let common = directories[0];
  for (const directory of directories.slice(1)) {
    let i = 0;
    while (i < common.length && i < directory.length && common[i] === directory[i]) i++;
    common = common.slice(0, i);
  }
  return common.join('/');
}

// The common directory of the files of a component, which is not cheap for big components
const commonDirectories = new WeakMap<ComponentJSON, string>();

/**
 * The path of a translation unit relative to its component: to the top level directory of the
 * component if it is known, otherwise to the directory that contains all of its files. This keeps
 * the absolute path of the project (e.g. the folders of the user) out of the file tree and the
 * breadcrumb.
 */
export function relativePath(component: ComponentJSON | null, unit: TranslationUnitJSON): string {
  const path = clean(unit.path);
  const top = component?.topLevel ? clean(component.topLevel).replace(/\/+$/, '') : '';
  let root = top && isUnder(path, top) ? top : '';
  if (!root && component) {
    let common = commonDirectories.get(component);
    if (common === undefined) {
      common = commonDirectory(component.translationUnits.map((u) => clean(u.path)));
      commonDirectories.set(component, common);
    }
    if (common && isUnder(path, common)) root = common;
  }
  return (root ? path.slice(root.length + 1) : path) || unit.name || 'root';
}
