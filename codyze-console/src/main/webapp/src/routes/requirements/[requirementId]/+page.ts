import type { PageLoad } from './$types';
import type { RequirementJSON } from '$lib/types';
import { throwError } from '$lib/errors';

export const load: PageLoad = async ({ fetch, params }) => {
  try {
    const response = await fetch(`/api/requirement/${params.requirementId}`).then(throwError);
    const requirement: RequirementJSON = await response.json();
    
    return { requirement };
  } catch (error) {
    console.error('Error loading requirement:', error);
    throw error;
  }
};
