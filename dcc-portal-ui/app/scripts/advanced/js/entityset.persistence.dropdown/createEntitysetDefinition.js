import _ from 'lodash';

import { ENTITY_TYPES, getDefaultSetSortOrder } from '../../../sets';

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
}) {
  const requiredFields = {filters, name, type, size};
  const missingRequiredFields = _.pickBy(requiredFields, _.isUndefined);
  invariant(!Object.keys(missingRequiredFields).length, `Required properties [${Object.keys(missingRequiredFields)}] cannot be undefined.`);
  invariant(_.values(ENTITY_TYPES).includes(type), `'type' must be one of [${_.values(ENTITY_TYPES)}]`);
  sortBy = sortBy || getDefaultSetSortOrder(type);

  sortOrder = sortOrder || SORT_ORDERS.DESCENDING;
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
