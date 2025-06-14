import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch }) => {
  try {
    const response = await fetch('/api/result');
    
    if (!response.ok) {
      return { components: [] };
    }
    
    const result = await response.json();
    return { components: result?.components || [] };
  } catch (error) {
    console.error('Error loading components:', error);
    return { components: [] };
  }
};