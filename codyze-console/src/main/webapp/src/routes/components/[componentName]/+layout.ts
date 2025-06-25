import { throwError } from '$lib/errors';
import type { ComponentJSON } from '$lib/types';
import type { LayoutLoad } from './$types';

export const load: LayoutLoad = async ({ fetch, params }) => {
  // Get the specific component
  const componentRes = await fetch(`/api/component/${params.componentName}`).then(throwError);
  const component: ComponentJSON = await componentRes.json();

  // Get all components for navigation
  let allComponents: ComponentJSON[] = [];
  try {
    const resultRes = await fetch('/api/result');
    if (resultRes.ok) {
      const result = await resultRes.json();
      allComponents = result?.components || [];
    }
  } catch (error) {
    console.error('Error loading all components:', error);
  }

  return { component, allComponents };
};
