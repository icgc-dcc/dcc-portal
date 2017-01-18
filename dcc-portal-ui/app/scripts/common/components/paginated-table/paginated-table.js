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
        const newOrderBy = column.sortFunction || column.dataFormat || column.field;
        invariant(newOrderBy, 'sortable column must have either a field, dataFormat, or sortFunction');
        this.orderBy = newOrderBy;
        this.sortOrder = (previousOrderBy === newOrderBy) ? _.xor([this.sortOrder], ['asc', 'desc'] )[0] : this.initialSortOrder;
      }
    };
  },
  controllerAs: 'vm',
  transclude: true,
  replace: true,
  template: `
    <div>
      <div class="t_table_top">
        <span>

          <span>
            Showing
              <span ng-show="vm.filteredRows().length > vm.itemsPerPage">
                <strong>{{vm.itemsPerPage * (vm.currentPageNumber - 1) + 1}}</strong>
                -
                <strong>{{vm.itemsPerPage * vm.currentPageNumber}}</strong>
                of
              </span>
              <strong>{{ vm.filteredRows().length }}</strong>
            files
          </span>

          <span
            ng-if="vm.shouldExportCSV"
            class="t_tools"
            data-toolbar=""
            data-dl="compound_mutated_genes"
          >
              <span
                class="t_tools__tool"
                tooltip-placement="left"
                tooltip="{{'Export Table as TSV' | translate}}"
              ><i class="icon-file"></i></span>
          </span>

          <table-filter
            class="small-filter"
            filter-model=""
            on-change="vm.handleFilterChange"
          ></table-filter>

        </span>
        <span data-ng-if="CompoundCtrl.getTargetedCompoundGenes() &&  CompoundCtrl.getTargetedCompoundGenes().length === 0">
            <translate>No targeted genes found.</translate>
        </span>
      </div>

      <table class="table">
        <thead>
          <th
            ng-repeat="column in vm.columns"
            ng-click="vm.handleClickTableHead(column)"
            bind-html-compile="column.headingFormat ? column.headingFormat(column.heading) : column.heading"
          >
          </th>
        </thead>
        <tbody>
          <tr ng-repeat="row in vm.currentPage()">
            <td
              ng-repeat="column in vm.columns"
              style="{{column.style}}"
              class="{{column.classes}}"
              bind-html-compile="column.dataFormat ? column.dataFormat(row[column.field], row, vm.columns) : row[column.field]"
            >
            </td>
          </tr>
        </tbody>
      </table>

      <div class="table-container" ng-transclude></div>

      <div
        ng-if="vm.filteredRows().length > vm.itemsPerPage"
        style="margin-top: 1rem"
      >
          <span>
            Showing
            <select
              ng-model="vm.itemsPerPage"
              ng-options="size for size in vm.itemsPerPageOptions"
            ></select>
            rows
          </span>

          <pagination
            total-items="vm.filteredRows().length"
            ng-model="vm.currentPageNumber"
            items-per-page="vm.itemsPerPage"
            max-size="5"
            rotate="true"
            class="pagination-sm pull-right"
          ></pagination>
      </div>
    </div>
  `,
});
