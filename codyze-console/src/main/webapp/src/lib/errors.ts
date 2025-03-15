import { error } from '@sveltejs/kit';

/**
 * Returns an {@link error} if an HTTP status other than OK is contained in
 * {@link response}. This will then automatically instruct sveltekit to display
 * our custom error page.
 *
 * @param response the original HTTP response
 *
 * @returns the response or an error
 */
export function throwError(response: Response) {
  if (!response.ok) {
    return error(response.status, response.statusText);
  }

  return response;
}
