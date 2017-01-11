import _ from 'lodash';
import jp from 'jsonpath-plus';

angular.module('icgc.compounds.index', [])
  .config(function ($stateProvider) {
    $stateProvider.state('compound-index', {
      url: '/compound',
      template: '<compound-index></compound-index>',
      reloadOnSearch: false,
    });
  })
  .service('CompoundIndexService', function (Restangular) {
    const getAll = () => Restangular.one('drugs').get({size: 100}).then(Restangular.stripRestangular);
    Object.assign(this, {
      getAll,
    });
  })
  .component('paginatedTable', {
    bindings: {
      rows: '<',
      itemsPerPage: '<',
      itemsPerPageOptions: '<',
      currentPageNumber: '<',
      /**
       * searchableJsonpaths contains an array of jsonpath query strings
       * see https://www.npmjs.com/package/jsonpath-plus for valid jsonpath queries
       */
      searchableJsonpaths: '<',
      filterFunction: '<',
    },
    controller: function ($filter) {
      this.filter = '';

      const defaultBindings = {
        rows: [],
        itemsPerPage: 10,
        itemsPerPageOptions: [10, 25, 50],
        currentPageNumber: 1,
      };

      const update = () => {
        _.defaults(this, defaultBindings);
      };

      const containsString = (target, query) => target.toLowerCase().includes(query.toLowerCase());
      this.filteredRows = () => {
        const filterExpression = (this.filterFunction
          ? this.filterFunction
          : this.searchableJsonpaths
            ? (row, rowIndex, array) => {
              const searchTargets = _.flattenDeep(this.searchableJsonpaths.map((path) => jp({json: row, path})));
              return _.some(searchTargets, target => target && containsString(target, this.filter))
            }
            : this.filter
        );
        return $filter('filter')(this.rows, filterExpression);
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

            <span class="t_tools" data-toolbar="" data-dl="compound_mutated_genes">
                <span
                  class="t_tools__tool"
                  tooltip-placement="left"
                  tooltip="{{'Export Table as TSV' | translate}}"
                  ng-clicks="downloadHTMLTable(dl, 'tsv')"
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
          <paginated-table
            rows="vm.compounds"
            searchable-jsonpaths="[
              '$.name',
              '$.zincId',
              '$.drugClass',
              '$.atcCodes[*].description',
            ]"
          >
            <table class="table">
              <tr ng-repeat="row in $parent.vm.currentPage()" style="">
                <td>
                  <span ng-bind-html="row.name | highlight: $parent.$parent.vm.filter"></span> 
                  (<span ng-bind-html="row.zincId | highlight: $parent.$parent.vm.filter"></span>)
                </td>
                <td ng-bind-html="row.drugClass | highlight: $parent.$parent.vm.filter"></td>

                <td ng-bind-html="row.atcCodes | _:'map':'description' | _:'join':',' | highlight: $parent.$parent.vm.filter"></td>

                <td ng-bind-html="row.cancerTrialCount | highlight: $parent.$parent.vm.filter"></td>
                <!--
                # genes targetd with bars
                100% width would be max length of 
                http://local.dcc.icgc.org:9000/api/v1/ui/search/gene-symbols/ENSG00000170827,ENSG00000095303
                -->
                <td>
                  {{ row.genes.length }}
                </td>
                <td>
                  {{ row.trials.length }}
                </td>
              </tr>
            </table>
          </paginated-table>
        </div>
      </div>
    `,
    controller: function (Page, CompoundIndexService) {
      Page.setTitle('Compounds');

      const update = async () => {
        this.compounds = await CompoundIndexService.getAll()
        console.log(this.compounds)
      };

      this.$onInit = update;
      this.$onChanges = update;
    },
    controllerAs: 'vm',
  })
  ;
