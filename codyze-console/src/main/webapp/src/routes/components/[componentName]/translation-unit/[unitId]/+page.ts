import { throwError } from '$lib/errors';
import type { NodeJSON, TranslationUnitJSON, ConceptInfo } from '$lib/types';
import { groupConcepts } from '$lib/concepts';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch, params, parent }) => {
  // Get component data from parent layout
  const { component, conceptGroups } = await parent();

  // General information about the translation unit.
  const translationUnit: TranslationUnitJSON = await fetch(
    `/api/component/${params.componentName}/translation-unit/${params.unitId}`
  )
    .then(throwError)
    .then((res) => res.json());

  // Out node information
  const astNodes: NodeJSON[] = await fetch(
    `/api/component/${params.componentName}/translation-unit/${params.unitId}/ast-nodes`
  )
    .then(throwError)
    .then((res) => res.json());
  const overlayNodes: NodeJSON[] = await fetch(
    `/api/component/${params.componentName}/translation-unit/${params.unitId}/overlay-nodes`
  )
    .then(throwError)
    .then((res) => res.json());

  return { translationUnit, astNodes, overlayNodes, conceptGroups, component };
};
