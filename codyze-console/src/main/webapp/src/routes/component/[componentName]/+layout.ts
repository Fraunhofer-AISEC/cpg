import { throwError } from '$lib/errors';
import type { ComponentJSON } from '$lib/types';
import type { LayoutLoad } from './$types';

export const load: LayoutLoad = async ({ fetch, params }) => {
  const res = await fetch(`/api/component/${params.componentName}`).then(throwError);
  const component: ComponentJSON = await res.json();

  return { component };
};
