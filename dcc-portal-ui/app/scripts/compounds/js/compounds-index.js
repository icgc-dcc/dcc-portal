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
  .directive('compile', ['$compile', function ($compile) {
      return function(scope, element, attrs) {
          var ensureCompileRunsOnce = scope.$watch(
            function(scope) {
               // watch the 'compile' expression for changes
              return scope.$eval(attrs.compile);
            },
            function(value) {
              // when the 'compile' expression changes
              // assign it into the current DOM
              element.html(value);

              // compile the new DOM and link it to the current
              // scope.
              // NOTE: we only compile .childNodes so that
              // we don't get into infinite loop compiling ourselves
              $compile(element.contents())(scope);
                
              // Use Angular's un-watch feature to ensure compilation only happens once.
              ensureCompileRunsOnce();
            }
        );
    };
  }])
  .component('paginatedTableWrapper', {
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
      orderBy: '<',
      sortOrder: '<',
    },
    controller: function ($filter) {
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
              const searchTargets = _.flattenDeep(searchableJsonpaths.map((path) => jp({json: row, path})));
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

      this.$onInit = update;
      this.$onChanges = update;

      this.handleFilterChange = (filter) => {
        this.filter = filter;
        this.currentPageNumber = 1;
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
          <paginated-table-wrapper
            rows="(vm.filteredCompounds) || vm.compounds"
            searchable-jsonpaths="[
              '$.name',
              '$.zincId',
              '$.drugClass',
              '$.atcCodes[*].description',
            ]"
            order-by="vm.orderBy"
            sort-order="vm.sortOrder"
            columns="vm.columns"
          >
            <table class="table">
              <thead>
                <th>
                  Name
                </th>
                <th>
                  ATC Level 4 Description
                </th>
                <th>
                  Compound Class
                </th>
                <th>
                  Targeted Genes
                </th>
                <th>
                  # Clinical Trials
                </th>
              </thead>
              <tbody>
                <tr ng-repeat="row in $parent.vm.currentPage()" style="">
                  <td>
                    <span ng-bind-html="row.name | highlight: $parent.$parent.vm.filter"></span> 
                    (<span ng-bind-html="row.zincId | highlight: $parent.$parent.vm.filter"></span>)
                  </td>

                  <td
                    collapsible-text
                    style="max-width: 200px;"
                    ng-bind-html="row.atcCodes | _:'map':'description' | _:'join':', ' | highlight: $parent.$parent.vm.filter"
                  ></td>

                  <td ng-bind-html="row.drugClass | highlight: $parent.$parent.vm.filter"></td>

                  <td>
                    {{ row.genes.length }}
                  </td>
                  <!--
                  # genes targetd with bars
                  100% width would be max length of 
                  http://local.dcc.icgc.org:9000/api/v1/ui/search/gene-symbols/ENSG00000170827,ENSG00000095303
                  -->

                  <td ng-bind-html="row.cancerTrialCount | highlight: $parent.$parent.vm.filter"></td>
                </tr>
              </tbody>
            </table>
          </paginated-table-wrapper>
        </div>
      </div>
    `,
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
      this.sortOrder = 'desc';

      this.filters = {};

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
