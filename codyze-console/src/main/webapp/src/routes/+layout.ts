import type { LayoutLoad } from './$types';
import { groupConcepts } from '$lib/concepts';

export const ssr = false;
import '@fontsource/noto-sans-mono';
import 'inter-ui/inter-variable.css';
import '../app.css';

export const load: LayoutLoad = async ({ fetch }) => {
  const res = await fetch('/api/classes/concepts');
  const data = await res.json();
  return { conceptGroups: groupConcepts(data.info) };
};
