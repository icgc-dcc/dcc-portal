import _ from 'lodash';

const ENTITY_TYPES = {
  DONOR: 'DONOR',
  GENE: 'GENE',
  MUTATION: 'MUTATION',
  FILE: 'FILE',
};

const SORT_ORDERS = {
  DESCENDING: "DESCENDING",
  ASCENDING: "ASCENDING", 
};

export class EntitysetDefinition {
  constructor ({
    filters,
    sortBy,
    sortOrder,
    name,
    description,
    type,
    size,
    isTransient
  } = {
    description: '',
    isTransient: false,
    sortOrder: SORT_ORDERS.DESCENDING,
  }) {
    invariant(_.values(ENTITY_TYPES).includes(type), `'type' must be one of ${ENTITY_TYPES}`);
    invariant(sortBy, 'required property sortBy is missing');
  }
}