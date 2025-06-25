import { throwError } from '$lib/errors';
import type { ComponentJSON } from '$lib/types';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch, params }) => {
  const component: ComponentJSON = await fetch(`/api/component/${params.componentName}`)
    .then(throwError)
    .then((res) => res.json());

  return { component };
};
