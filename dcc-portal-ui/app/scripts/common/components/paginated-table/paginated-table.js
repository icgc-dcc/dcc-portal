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
    sortColumnId: '<',
    sortOrder: '<',
    shouldExportCSV: '<',
    stickyHeader: '<',
    onChange: '&',
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
      const column = _.find(this.columns, {id: this.sortColumnId}) || this.columns[0];
      const iteratee = column.sortFunction || column.field;
      return _.orderBy(filteredRows, iteratee, this.sortOrder);
    };

    this.pages = () => {
      return _.chunk(this.filteredRows(), this.itemsPerPage);
    };

    this.currentPage = () => {
      return this.pages()[this.currentPageNumber - 1];
    };

    this.$onInit = function () {
      update();
    };

    this.$onChanges = update;

    this.handleItemsPerPageChange = (itemsPerPage) => {
      this.currentPageNumber = 1;
    };

    this.handleFilterChange = (filter) => {
      this.filter = filter;
      this.currentPageNumber = 1;
    };

    this.handleClickTableHead = (column) => {
      if (column.isSortable) {
        this.sortOrder = (column.id === this.sortColumnId) ? _.xor([this.sortOrder], ['asc', 'desc'] )[0] : this.sortOrder;
        this.sortColumnId = column.id;
        this.currentPageNumber = 1;
      }
    };

    this.isSortingOnColumn = (column) => column.id === this.sortColumnId;

    $scope.$watchGroup([
        () => this.sortColumnId,
        () => this.sortOrder,
        () => this.currentPageNumber,
        () => this.itemsPerPage,
      ], () => {
      (this.onChange() || _.noop)({
        sortColumnId: this.sortColumnId,
        sortOrder: this.sortOrder,
        currentPageNumber: this.currentPageNumber,
        itemsPerPage: this.itemsPerPage,
      });
    });
  },
  controllerAs: 'vm',
  transclude: true,
  replace: true,
  template: require('./paginated-table.html'),
});
