// QueryTree store for lazy loading and caching
import { writable } from 'svelte/store';
import type { QueryTreeJSON } from '$lib/types';

// Cache for loaded QueryTrees
const queryTreeCache = new Map<string, QueryTreeJSON>();

// Loading states
const loadingStates = writable<Set<string>>(new Set());

// Error states
const errors = writable<Map<string, string>>(new Map());

/**
 * Loads multiple QueryTrees by IDs using batch API
 */
export async function loadQueryTrees(queryTreeIds: string[]): Promise<QueryTreeJSON[]> {
  // Filter out already cached ones
  const uncachedIds = queryTreeIds.filter(id => !queryTreeCache.has(id));
  
  if (uncachedIds.length === 0) {
    // All are cached, return from cache
    return queryTreeIds.map(id => queryTreeCache.get(id)!).filter(Boolean);
  }

  // Mark all as loading
  loadingStates.update(states => {
    const newStates = new Set(states);
    uncachedIds.forEach(id => newStates.add(id));
    return newStates;
  });

  try {
    const response = await fetch('/api/querytrees', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(uncachedIds),
    });

    if (!response.ok) {
      throw new Error(`Failed to load QueryTrees: ${response.statusText}`);
    }

    const queryTrees: QueryTreeJSON[] = await response.json();
    
    // Cache all results
    queryTrees.forEach(queryTree => {
      queryTreeCache.set(queryTree.id, queryTree);
    });

    // Clear any previous errors
    errors.update(errorMap => {
      const newMap = new Map(errorMap);
      uncachedIds.forEach(id => newMap.delete(id));
      return newMap;
    });

    // Return all requested QueryTrees (cached + newly loaded)
    return queryTreeIds.map(id => queryTreeCache.get(id)!).filter(Boolean);

  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    
    // Store errors for all failed IDs
    errors.update(errorMap => {
      const newMap = new Map(errorMap);
      uncachedIds.forEach(id => newMap.set(id, errorMessage));
      return newMap;
    });

    console.error('Failed to load QueryTrees:', error);
    return [];

  } finally {
    // Remove all from loading states  
    loadingStates.update(states => {
      const newStates = new Set(states);
      uncachedIds.forEach(id => newStates.delete(id));
      return newStates;
    });
  }
}

/**
 * Gets a QueryTree from cache (synchronous)
 */
export function getCachedQueryTree(queryTreeId: string): QueryTreeJSON | null {
  return queryTreeCache.get(queryTreeId) || null;
}

/**
 * Checks if a QueryTree is currently loading
 */
export function isQueryTreeLoading(queryTreeId: string): boolean {
  let isLoading = false;
  loadingStates.subscribe(states => {
    isLoading = states.has(queryTreeId);
  })();
  return isLoading;
}

/**
 * Gets error for a QueryTree if any
 */
export function getQueryTreeError(queryTreeId: string): string | null {
  let error: string | null = null;
  errors.subscribe(errorMap => {
    error = errorMap.get(queryTreeId) || null;
  })();
  return error;
}

/**
 * Pre-loads QueryTrees into cache (useful for root QueryTrees from requirements)
 */
export function preloadQueryTree(queryTree: QueryTreeJSON): void {
  queryTreeCache.set(queryTree.id, queryTree);
}

/**
 * Clears the QueryTree cache (useful when starting new analysis)
 */
export function clearQueryTreeCache(): void {
  queryTreeCache.clear();
  loadingStates.set(new Set());
  errors.set(new Map());
}

// Export the loading states and errors as stores for reactive components
export { loadingStates, errors };
