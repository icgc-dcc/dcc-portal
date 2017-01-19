import jp from 'jsonpath-plus';
import memoize from 'memoizee';
import _ from 'lodash';

angular.module('app.common.components')
.component('paginatedTable', {
  bindings: {
    rows: '<',
    columns: '<',
    itemsPerPage: '<',
    itemsPerPageOptions: '<',
    currentPageNumber: '<',
    /**
     * searchableJsonpaths contains an array of jsonpath query strings
     * see https://www.npmjs.com/package/jsonpath-plus for valid jsonpath queries
     */
    searchableJsonpaths: '<',
    filterFunction: '<',
    initialOrderBy: '<',
    initialSortOrder: '<',
    shouldExportCSV: '<',
    stickyHeader: '<',
  },
  controller: function ($filter, $compile, $scope) {
    this.filter = '';

    const defaultBindings = {
      rows: [],
      itemsPerPage: 10,
      itemsPerPageOptions: [10, 25, 50],
      currentPageNumber: 1,
      sortOrder: 'asc',
      shouldExportCSV: false,
    };

    let getFilteredRows;

    const update = () => {
      _.defaults(this, defaultBindings);
      if (getFilteredRows && getFilteredRows.clear) {
        getFilteredRows.clear();
      }
      getFilteredRows = memoize((filter) => {
        const filterExpression = getFilterExpression(this.filterFunction, this.searchableJsonpaths, filter);
        return $filter('filter')(this.rows, filterExpression);
      });
    };

    const containsString = (target, query) => target.toLowerCase().includes(query.toLowerCase());

    const getFilterExpression = (filterFunction, searchableJsonpaths, filter) => (filterFunction
      ? filterFunction
      : searchableJsonpaths
        ? (row, rowIndex, array) => {
          const searchTargets = _.flattenDeep(searchableJsonpaths.map((path) => jp({ json: row, path })));
          return _.some(searchTargets, target => target && containsString(target, filter))
        }
        : filter
    );

    this.filteredRows = () => {
      const filteredRows = getFilteredRows(this.filter);
      return this.orderBy ? _.orderBy(filteredRows, this.orderBy, this.sortOrder) : filteredRows;
    };

    this.pages = () => {
      return _.chunk(this.filteredRows(), this.itemsPerPage);
    };

    this.currentPage = () => {
      return this.pages()[this.currentPageNumber - 1];
    };

    this.$onInit = function () {
      update();
      _.extend(this, {
        orderBy: this.initialOrderBy,
        sortOrder: this.initialSortOrder,
      });
    }
    this.$onChanges = update;

    this.handleFilterChange = (filter) => {
      this.filter = filter;
      this.currentPageNumber = 1;
    };

    this.handleClickTableHead = (column) => {
      if (column.isSortable) {
        const previousOrderBy = this.orderBy;
        const newOrderBy = column.sortFunction || column.field;
        invariant(newOrderBy, 'sortable column must have either a field or sortFunction');
        this.orderBy = newOrderBy;
        this.sortOrder = (previousOrderBy === newOrderBy) ? _.xor([this.sortOrder], ['asc', 'desc'] )[0] : this.initialSortOrder;
        this.currentPageNumber = 1;
      }
    };

    this.isSortingOnColumn = (column) => [column.sortFunction, column.field].includes(this.orderBy);
  },
  controllerAs: 'vm',
  transclude: true,
  replace: true,
  template: require('./paginated-table.html'),
});
