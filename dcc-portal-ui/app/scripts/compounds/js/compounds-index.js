import _ from 'lodash';
import memoize from 'memoizee';
import { highlightFn } from '../../common/js/common';

const filterParams = [
  'name',
  'gene',
  'atc',
  'clinicalTrialCondition',
  'drugClass',
];

const paginatedTableParams = [
  'sortColumnId',
  'sortOrder',
  'currentPageNumber',
  'itemsPerPage',
  'tableFilter',
];

const routeParams = [
  ...filterParams,
  ...paginatedTableParams
];

angular.module('icgc.compounds.index', [])
  .config(function ($stateProvider) {
    $stateProvider.state('compound-index', {
      url: `/compounds?${routeParams.join('&')}`,
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
  .component('compoundIndex', {
    template: `
      <div class="compound-index">
        <div class="h1-wrap" >
          <h1 data-ui-scrollpoint="79">
            <span class="t_badge t_badge__compound"></span>
            <span>Compounds</span>
          </h1>
        </div>
        <div class="content">
          <aside class="t_sidebar">
            <collapsible-wrapper
              title="'Compound'"
            >
              <input
                class="t_input__block"
                type="search"
                ng-model="vm.filters.compoundName"
                ng-change="vm.handleFiltersChange(vm.filters)"
                placeholder="e.g. Aspirin, ZINC00003376543"
              >
            </collapsible-wrapper>

            <collapsible-wrapper
              title="'Targeted Gene'"
            >
              <input
                class="t_input__block"
                ng-model="vm.filters.gene"
                ng-change="vm.handleFiltersChange(vm.filters)"
                placeholder="{{ vm.isLoadingGeneSymbolMap ? 'Loading Gene Symbols' : 'e.g. BRAF, ENSG00000144891'}}"
                ng-disabled="vm.isLoadingGeneSymbolMap"
              ></input>
            </collapsible-wrapper>

            <collapsible-wrapper
              title="'ATC Code / Description'"
            >
              <input
                class="t_input__block"
                ng-model="vm.filters.compoundAtc"
                ng-change="vm.handleFiltersChange(vm.filters)"
                placeholder="e.g. L01X1, kinase inhibitors"
              ></input>
            </collapsible-wrapper>

            <collapsible-wrapper
              title="'Clinical Trial Condition'"
            >
              <input
                class="t_input__block"
                ng-model="vm.filters.compoundCTC"
                ng-change="vm.handleFiltersChange(vm.filters)"
                placeholder="e.g. leukemia, ovarian"
              ></input>
            </collapsible-wrapper>
            <collapsible-wrapper
              title="'Class'"
            >

              <togglable-term
                ng-repeat="term in [
                    {code: 'fda', title: 'FDA'},
                    {code: 'world', title: 'World'},
                ]"
                on-click="vm.toggleFacetContent('compoundDrugClass', term.code)"
                is-active="vm.filters.compoundDrugClass.includes(term.code)"
                label="term.title"
                items-affected-by-facet="vm.getFilteredCompounds(vm.compounds, _.omit(vm.filters, 'drugClass')).length"
                items-affected-by-term="_.filter(vm.getFilteredCompounds(vm.compounds, _.omit(vm.filters, 'drugClass')), {drugClass: term.code}).length"
              ></togglable-term>

            </collapsible-wrapper>
          </aside>
          <article>
            <div class="t_current" data-ng-if="!_.isEmpty(vm.filters)">
              <ul>
                <li>
                  <button type="button" class="t_button t_current__remove_all" data-ng-click="vm.removeAllFilters()">
                      <i class="icon-undo"></i>
                  </button>
                  <share-button></share-button>
                </li>
                <li class="t_facets__facet"
                  data-ng-repeat="(key, value) in vm.filters track by key"
                  data-ng-if="value.length">
                  <span class="t_facets__facet__label"
                    data-ng-mouseenter="hoverStyle={'text-decoration':'line-through'}; $event.stopPropagation();"
                    data-ng-mouseleave="hoverStyle={}; $event.stopPropagation()"
                    data-ng-click="vm.removeFilter(key)">
                    {{ key | trans }}
                  </span>
                  <span class="t_current__or">{{ _.isArray(value) && value.length > 1 ? 'IN (' : 'IS' }}</span>
                  <ul class="t_facets__facet__terms">
                    <li class="t_facets__facet__terms__term" data-ng-if="!_.isArray(value)">
                      <span class="t_facets__facet__terms__active__term__label"
                        data-ng-style="hoverStyle"
                        data-ng-click="vm.removeFilter(key, value)">
                        {{ value | _:'upperFirst' }}
                      </span>
                    </li>
                    <li data-ng-if="_.isArray(value)"
                      data-ng-repeat="(index, term) in value track by index" class="t_facets__facet__terms__term">
                      <span class="t_facets__facet__terms__active__term__label"
                        data-ng-style="hoverStyle"
                        data-ng-click="vm.removeFilter(key, term)">
                          {{ term | _:'upperFirst' }}
                      </span>
                      <span data-ng-if="!$last" class="t_current__or">,&nbsp;</span>
                      <span data-ng-if="$last && value.length > 1" class="t_current__or">)</span>
                    </li>
                  </ul>
                  <span data-ng-if="!$last" class="t_current__and"> AND </span>
                </li>
              </ul>
            </div>
            <section ng-class="{loading: vm.isLoading}">
              <h3 ng-if="vm.isLoading">
                  <i class="icon-spinner icon-spin"></i> <translate>Loading Compounds...</translate>
              </h3>
              <div ng-if="!vm.isLoading">
                <paginated-table
                  rows="(vm.filteredCompounds) || vm.compounds"
                  searchable-jsonpaths="[
                    '$.name',
                    '$.zincId',
                    '$.drugClass',
                    '$.atcCodes[*].description',
                  ]"
                  table-filter="vm.tableState.tableFilter"
                  sort-column-id="vm.tableState.sortColumnId"
                  sort-order="vm.tableState.sortOrder"
                  items-per-page="vm.tableState.itemsPerPage"
                  current-page-number="vm.tableState.currentPageNumber"
                  columns="vm.columns"
                  sticky-header="true"
                  on-change="vm.handlePaginatedTableChange"
                  item-type-name="'compound'"
                >
                </paginated-table>
              </div>
            </section>
          </article>
        </div>
      </div>
    `,
    // # genes targetd with bars
    // 100% width would be max length of 
    // http://local.dcc.icgc.org:9000/api/v1/ui/search/gene-symbols/ENSG00000170827,ENSG00000095303
    controller: function (Page, CompoundIndexService, $location, $scope, GeneSymbols) {
      Page.setTitle('Compounds');
      Page.setPage('entity');

      this.isLoading = false;
      $scope._ = _;

      const update = _.debounce(async () => {
        this.isLoading = true;
        this.compounds = await CompoundIndexService.getAll();
        this.isLoading = false;
        this.handleFiltersChange(this.filters);
      });

      this.isLoadingGeneSymbolMap = true;
      GeneSymbols.getAll().then(geneSymbols => {
        this.isLoadingGeneSymbolMap = false;
        this.geneSymbols = geneSymbols;
      });

      this.$onInit = update;
      this.$onChanges = update;

      const defaultFiltersState = {
        compoundName: '',
        gene: '',
        compoundAtc: '',
        compoundCTC: '',
        compoundDrugClass: [],
      };

      const defaultTableState = {
        sortColumnId: 'geneCount',
        sortOrder: 'desc',
        currentPageNumber: 1,
        itemsPerPage: 10,
        tableFilter: '',
      };

      this.filters = _.defaults(_.pick($location.search(), filterParams), {});
      this.tableState = _.mapValues(_.defaults(_.pick($location.search(), paginatedTableParams), defaultTableState), (value, key) => _.isNumber(defaultTableState[key]) ? _.toNumber(value) : value);

      this.columns = [
        {
          id: 'name',
          heading: 'Name',
          isSortable: true,
          sortFunction: (row) => `${row.name} (${row.zincId})`,
          dataFormat: (cell, row, array, extraData) => {
            const { tableFilter } = extraData;
            const unformattedContent = `${row.name} (${row.zincId})`;
            const content = [tableFilter, this.filters.compoundName]
              .sort((a, b) => b.length - a.length)
              .filter(Boolean)
              .reduce((content, query) => highlightFn(content, query), unformattedContent) || unformattedContent;
            return `
              <a ui-sref="compound({compoundId: '${row.zincId}'})">
                ${content}
              </a>
            `;
          }
        },
        {
          id: 'atc',
          heading: 'ATC Level 4 Description',
          style: 'max-width: 200px',
          dataFormat: (cell, row, array, extraData) => {
            const { tableFilter } = extraData;
            const unformattedContent = _.map(row.atcCodes, 'description').join(', ');
            const content = [tableFilter, this.filters.compoundAtc]
              .sort((a, b) => b.length - a.length)
              .filter(Boolean)
              .reduce((content, query) => highlightFn(content, query), unformattedContent) || unformattedContent;
            return `
              <div collapsible-text>
                ${content}
              </div>
            `;
          },
        },
        {
          id: 'compoundClass',
          heading: 'Compound Class',
          isSortable: true,
          field: 'drugClass',
          dataFormat: (cell, row, array, extraData) => {
            const { tableFilter } = extraData;
            const content = { fda: 'FDA', world: 'World' }[cell];
            return highlightFn(content, tableFilter);
          }
        },
        {
          id: 'geneCount',
          heading: '# Targeted Genes',
          classes: 'text-right',
          isSortable: true,
          sortFunction: (row) => row.genes.length,
          dataFormat: (cell, row, array) => {
            const filtersValue = JSON.stringify({gene:{compoundId:{is: [row.zincId] }}})
              .replace(/"/g, '&quot;');
            return `
              <a ui-sref="advanced.gene({filters: '${filtersValue}'})">
                ${row.genes.length.toLocaleString()}
              </a>
            `;
          }
        },
        {
          id: 'trialCount',
          heading: '# Clinical Trials',
          classes: 'text-right',
          isSortable: true,
          sortFunction: (row) => row.trials.length,
          dataFormat: (cell, row, array) => {
            return `
              <a ui-sref="compound({compoundId: '${row.zincId}', '#': 'trials'})">
                ${row.trials.length.toLocaleString()}
              </a>
            `;
          }
        },
      ];
      
      this.toggleFacetContent = (facet, classType) => {
        this.filters[facet] = _.xor((this.filters[facet] || []), [classType]);
        this.handleFiltersChange(this.filters);
      };

      this.getFilteredCompounds = (compounds, filters) => {
        const nameRegex = new RegExp(filters.compoundName, 'i');
        const atcRegex = new RegExp(filters.compoundAtc, 'i');
        const geneRegex = new RegExp(filters.gene, 'i');
        const clinicalTrialConditionRegex = new RegExp(filters.compoundCTC, 'i');

        return _.filter(compounds, (compound) => {
          return _.every([
            filters.compoundName ? (compound.name.match(nameRegex) || compound.zincId.match(nameRegex)) : true,
            filters.gene ? (_.some(compound.genes, item => _.some(_.map(item, (value, key) => (value.match(geneRegex) || (key === 'ensemblGeneId' && this.geneSymbols && this.geneSymbols[value] && this.geneSymbols[value].match(geneRegex))))))) : true,
            filters.compoundAtc ? (_.some(compound.atcCodes, item => _.some(_.values(item).map(value => value.match(atcRegex))))) : true,
            filters.compoundCTC ? (_.some(_.flattenDeep(compound.trials.map(trial => trial.conditions.map(_.values))), str => str.match(clinicalTrialConditionRegex))) : true,
            filters.compoundDrugClass && filters.compoundDrugClass.length ? filters.compoundDrugClass.includes(compound.drugClass) : true,
          ]);
        });
      };

      const getCombinedState = () => Object.assign({},
        _.omitBy(this.filters, (value, key) => _.isEqual(defaultFiltersState[key], value)),
        _.omitBy(this.tableState, (value, key) => _.isEqual(defaultTableState[key], value)),
      );

      this.handleFiltersChange = (filters) => {
        _.each(filters, (value, key) => {
          if(_.isEmpty(value)) {
            delete filters[key];
          }
        });
        this.filteredCompounds = this.getFilteredCompounds(this.compounds, filters);
        $location.replace();
        $location.search(getCombinedState());
      };

      this.handlePaginatedTableChange = (newTableState) => {
        this.tableState = newTableState;
        $location.replace();
        $location.search(getCombinedState());
      };

      this.removeFilter = (key, value) => {
        if(key === 'compoundDrugClass' && value && this.filters[key].length > 1) {
          _.remove(this.filters[key], (v) => v === value);
        } else {
          delete this.filters[key];
        }
        this.handleFiltersChange(this.filters);
      };

      this.removeAllFilters = () => {
        this.filters = {};
        this.tableState = _.clone(defaultTableState);
        this.handleFiltersChange(this.filters);
        this.handlePaginatedTableChange(this.tableState);
      };
    },
    controllerAs: 'vm',
  });
