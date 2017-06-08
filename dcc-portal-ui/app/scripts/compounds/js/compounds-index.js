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
                ng-model="vm.filters.name"
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
                ng-model="vm.filters.atc"
                ng-change="vm.handleFiltersChange(vm.filters)"
                placeholder="e.g. L01X1, kinase inhibitors"
              ></input>
            </collapsible-wrapper>

            <collapsible-wrapper
              title="'Clinical Trial Condition'"
            >
              <input
                class="t_input__block"
                ng-model="vm.filters.clinicalTrialCondition"
                ng-change="vm.handleFiltersChange(vm.filters)"
                placeholder="e.g. leukemia, ovarian"
              ></input>
            </collapsible-wrapper>

            <collapsible-wrapper
              title="'Class'"
            >

              <togglable-term
                ng-repeat="term in vm.getSortedTerms({
                  terms: [
                    {code: 'fda', title: 'FDA'},
                    {code: 'world', title: 'World'},
                  ],
                  facet: vm.filters.drugClass
                })"
                on-click="vm.toggleFacetContent('drugClass', term.code)"
                is-active="vm.filters.drugClass.includes(term.code)"
                label="term.title"
                items-affected-by-facet="vm.getFilteredCompounds(vm.compounds, _.omit(vm.filters, 'drugClass')).length"
                items-affected-by-term="_.filter(vm.getFilteredCompounds(vm.compounds, _.omit(vm.filters, 'drugClass')), {drugClass: term.code}).length"
              ></togglable-term>

            </collapsible-wrapper>
          </aside>
          <article>
            <section ng-class="{loading: vm.isLoading}">
              <h3 ng-if="vm.isLoading">
                  <i class="icon-spinner icon-spin"></i> <translate>Loading Compounds...</translate>
              </h3>
              <div ng-if="!vm.isLoading">
                <share-button></share-button>
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
        name: '',
        gene: '',
        atc: '',
        clinicalTrialCondition: '',
        drugClass: [],
      };

      const defaultTableState = {
        sortColumnId: 'geneCount',
        sortOrder: 'desc',
        currentPageNumber: 1,
        itemsPerPage: 10,
        tableFilter: '',
      };

      this.filters = _.defaults(_.pick($location.search(), filterParams), defaultFiltersState);
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
            const content = [tableFilter, this.filters.name]
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
            const content = [tableFilter, this.filters.atc]
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
            return highlightFn(content, tableFilter)
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
        const nameRegex = new RegExp(filters.name, 'i');
        const atcRegex = new RegExp(filters.atc, 'i');
        const geneRegex = new RegExp(filters.gene, 'i');
        const clinicalTrialConditionRegex = new RegExp(filters.clinicalTrialCondition, 'i');

        return _.filter(compounds, (compound) => {
          return _.every([
            filters.name ? (compound.name.match(nameRegex) || compound.zincId.match(nameRegex)) : true,
            filters.gene ? (_.some(compound.genes, item => _.some(_.map(item, (value, key) => (value.match(geneRegex) || (key === 'ensemblGeneId' && this.geneSymbols && this.geneSymbols[value] && this.geneSymbols[value].match(geneRegex))))))) : true,
            filters.atc ? (_.some(compound.atcCodes, item => _.some(_.values(item).map(value => value.match(atcRegex))))) : true,
            filters.clinicalTrialCondition ? (_.some(_.flattenDeep(compound.trials.map(trial => trial.conditions.map(_.values))), str => str.match(clinicalTrialConditionRegex))) : true,
            filters.drugClass && filters.drugClass.length ? filters.drugClass.includes(compound.drugClass) : true,
          ]);
        });
      };

      this.getSortedTerms = memoize(({terms, facet}) => {
        return _.orderBy(terms, term => !facet.includes(term.code));
      }, {normalizer: (args) => JSON.stringify(args)});

      const getCombinedState = () => Object.assign({},
        _.omitBy(this.filters, (value, key) => _.isEqual(defaultFiltersState[key], value)),
        _.omitBy(this.tableState, (value, key) => _.isEqual(defaultTableState[key], value)),
      );

      this.handleFiltersChange = (filters) => {
        this.filteredCompounds = this.getFilteredCompounds(this.compounds, filters);
        $location.replace();
        $location.search(getCombinedState());
      };

      this.handlePaginatedTableChange = (newTableState) => {
        this.tableState = newTableState;
        $location.replace();
        $location.search(getCombinedState());
      };
    },
    controllerAs: 'vm',
  })
  ;
