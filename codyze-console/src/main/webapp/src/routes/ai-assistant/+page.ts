import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch }) => {
  try {
    const response = await fetch('/api/result');
    const result = await response.json();
    return {
      components: result.components || []
    };
  } catch (error) {
    console.error('Failed to load components:', error);
    return {
      components: null
    };
  }
};
