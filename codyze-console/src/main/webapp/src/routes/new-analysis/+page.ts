import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch }) => {
  // No data loading needed for the new analysis page
  return {};
};
