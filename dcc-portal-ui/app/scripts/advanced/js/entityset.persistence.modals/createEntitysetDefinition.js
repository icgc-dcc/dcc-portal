import _ from 'lodash';

const ENTITY_TYPES = {
  DONOR: 'DONOR',
  GENE: 'GENE',
  MUTATION: 'MUTATION',
  FILE: 'FILE',
};

const SORT_ORDERS = {
  DESCENDING: 'DESCENDING',
  ASCENDING: 'ASCENDING', 
};

export default function createEntitysetDefinition ({
  filters,
  sortBy,
  sortOrder,
  name,
  type,
  size,
  isTransient,
} = {
  isTransient: false
}) {
  const requiredFields = {filters, name, type, size};
  const missingRequiredFields = _.pickBy(requiredFields, _.isUndefined);
  invariant(!Object.keys(missingRequiredFields).length, `Required properties [${Object.keys(missingRequiredFields)}] cannot be undefined.`);
  invariant(_.values(ENTITY_TYPES).includes(type), `'type' must be one of [${_.values(ENTITY_TYPES)}]`);
  return {
    filters,
    sortBy,
    sortOrder,
    name,
    type,
    size,
    isTransient,
  };
}
