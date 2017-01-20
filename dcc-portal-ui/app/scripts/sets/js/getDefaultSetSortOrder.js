import invariant from 'invariant';
import { ENTITY_TYPES } from './ENTITY_TYPES';

export const getDefaultSetSortOrder = (setType) => {
  // invariant(_.values(ENTITY_TYPES).includes(setType), `'type' must be one of [${_.values(ENTITY_TYPES)}]`);
  return {
    [ENTITY_TYPES.FILE]: 'id',
    [ENTITY_TYPES.DONOR]: 'ssmAffectedGenes',
    [ENTITY_TYPES.MUTATION]: 'affectedDonorCountFiltered',
    [ENTITY_TYPES.GENE]: 'affectedDonorCountFiltered',
  }[setType];
};