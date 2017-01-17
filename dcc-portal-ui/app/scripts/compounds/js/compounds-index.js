import _ from 'lodash';
import memoize from 'memoizee';
import jp from 'jsonpath-plus';
import insertCss from 'insert-css';

angular.module('icgc.compounds.index', [])
  .config(function ($stateProvider) {
    $stateProvider.state('compound-index', {
      url: '/compound',
      template: '<compound-index></compound-index>',
      reloadOnSearch: false,
    });
  })
  .service('CompoundIndexService', function (Restangular) {
    const params = {
      size: 2000,
    };
    const getAll = () => Restangular.one('drugs').get(params).then(Restangular.stripRestangular);
    Object.assign(this, {
      getAll,
    });
  })
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
    },
    controller: function ($filter, $compile, $scope) {
      this.filter = '';

      const defaultBindings = {
        rows: [],
        itemsPerPage: 10,
        itemsPerPageOptions: [10, 25, 50],
        currentPageNumber: 1,
        sortOrder: 'asc'
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
          if (column.sortFunction) {
            this.orderBy = column.sortFunction;
          } else {
            this.orderBy = column.dataFormat || column.field;
            if (!this.orderBy) {
              console.warn('missing value for this.orderBy');
            }
          }
          this.sortOrder = (previousOrderBy === this.orderBy) ? _.xor([this.sortOrder], ['asc', 'desc'] )[0] : this.initialSortOrder;
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

            <!--
            <span
              class="t_tools"
              data-toolbar=""
              data-dl="compound_mutated_genes"
            >
                <span
                  class="t_tools__tool"
                  tooltip-placement="left"
                  tooltip="{{'Export Table as TSV' | translate}}"
                  ng-clicks="downloadHTMLTable(dl, 'tsv')"
                ><i class="icon-file"></i></span>
            </span>
            -->

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
  })
  .directive('collapsibleText', function () {
    insertCss(`
      .collapsible-text--collapsed {
        text-overflow: ellipsis;
        white-space: nowrap !important;
        overflow: hidden !important;
      }
    `);
    return {
      restrict: 'AE',
      link: function (scope, $element) {
        $element.addClass('collapsible-text collapsible-text--collapsed')
        $element.click(() => $element.toggleClass('collapsible-text--expanded collapsible-text--collapsed'));
      }
    };
  })
  .component('compoundIndex', {
    template: `
      <div>
        <div class="h1-wrap">
          <h1 data-ui-scrollfix="79">
            <span class="t_badge t_badge__compound"></span>
            <span>Compounds</span>
          </h1>
        </div>
        <div>
          <div>
            Compound
            <input
              ng-model="vm.filters.name"
              ng-change="vm.handleFiltersChange(vm.filters)"
            ></input>
          </div>
          <div>
            Targeted Gene
            <input
              ng-model="vm.filters.gene"
              ng-change="vm.handleFiltersChange(vm.filters)"
            ></input>
          </div>
          <div>
            ATC Code / Description
            <input
              ng-model="vm.filters.atc"
              ng-change="vm.handleFiltersChange(vm.filters)"
            ></input>
          </div>
          <div>
            Clinical Trial Condition
            <input
              ng-model="vm.filters.clinicalTrialCondition"
              ng-change="vm.handleFiltersChange(vm.filters)"
            ></input>
          </div>
        </div>
        <div>
          <paginated-table
            rows="(vm.filteredCompounds) || vm.compounds"
            searchable-jsonpaths="[
              '$.name',
              '$.zincId',
              '$.drugClass',
              '$.atcCodes[*].description',
            ]"
            initial-order-by="vm.orderBy"
            initial-sort-order="'desc'"
            columns="vm.columns"
          >
          </paginated-table>
        </div>
      </div>
    `,
    // # genes targetd with bars
    // 100% width would be max length of 
    // http://local.dcc.icgc.org:9000/api/v1/ui/search/gene-symbols/ENSG00000170827,ENSG00000095303
    controller: function (Page, CompoundIndexService) {
      Page.setTitle('Compounds');
      Page.setPage('entity');

      const update = _.debounce(async () => {
        this.compounds = await CompoundIndexService.getAll();
        console.log('compounds from service', this.compounds)
        this.handleFiltersChange(this.filters);
      });

      this.$onInit = update;
      this.$onChanges = update;

      this.orderBy = (row) => row.genes.length;

      this.filters = {};

      this.columns = [
        {
          heading: 'Name',
          dataFormat: (cell, row, array) => {
            return `${row.name} (${row.zincId})`;
          }
        },
        {
          heading: 'ATC Level 4 Description',
          style: 'max-width: 200px',
          dataFormat: (cell, row, array) => {
            return `<div collapsible-text>
              ${_.map(row.atcCodes, 'description').join(', ')}
            </div>`;
          },
        },
        {
          heading: 'Compound Class',
          field: 'drugClass',
          dataFormat: (cell, row, array) => {
            return { fda: 'FDA', world: 'World' }[cell];
          }
        },
        {
          heading: '# Targed Genes',
          classes: 'text-right',
          dataFormat: (cell, row, array) => {
            return row.genes.length;
          }
        },
        {
          heading: '# Clinical Trials',
          classes: 'text-right',
          isSortable: true,
          sortFunction: (row) => row.trials.length,
          dataFormat: (cell, row, array) => {
            return row.trials.length;
          }
        },
      ];

      this.handleFiltersChange = (filters) => {
        const nameRegex = new RegExp(this.filters.name, 'i');
        const atcRegex = new RegExp(this.filters.atc, 'i');
        const clinicalTrialConditionRegex = new RegExp(this.filters.clinicalTrialCondition, 'i');
        this.filteredCompounds = _.filter(this.compounds, (compound) => {
          return _.every([
            this.filters.name ? (compound.name.match(nameRegex) || compound.zincId.match(nameRegex)) : true,
            this.filters.atc ? (_.some(compound.atcCodes, item => _.some(_.values(item).map(value => value.match(atcRegex))))) : true,
            this.filters.clinicalTrialCondition ? (_.some(_.flattenDeep(compound.trials.map(trial => trial.conditions.map(_.values))), str => str.match(clinicalTrialConditionRegex))) : true,
          ]);
        });
      }
    },
    controllerAs: 'vm',
  })
  ;
