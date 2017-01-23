import _ from 'lodash';

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
  'itemsPerPage'
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
  .component('collapsibleWrapper', {
    bindings: {
      title: '<',
      isCollapsed: '<'
    },
    transclude: true,
    controller: function () {
    },
    controllerAs: 'vm',
    template: `
      <ul class="t_facets__facet">
        <li
          class="t_facets__facet__title"
          ng-click="vm.isCollapsed = !vm.isCollapsed"
        >
          <span class="t_facets__facet__title__label">
            <i
              data-ng-class="{
                'icon-caret-down': !vm.isCollapsed,
                'icon-caret-right': vm.isCollapsed,
            }"></i>
            {{vm.title}}
          </span>
        </li>
        <li
          class=""
          ng-transclude
          ng-show="!vm.isCollapsed"
        ></li>
      </ul>
    `
  })
  .component('togglableTerm', {
    bindings: {
      isActive: '<',
      onClick: '&',
      label: '<',
      itemsAffectedByFacet: '<',
      itemsAffectedByTerm: '<',
    },
    transclude: true,
    controller: function () {},
    controllerAs: 'vm',
    template: `
      <div
        ng-class="{
          'is-active': vm.isActive,
          't_facets__facet__terms__active__term__label': vm.isActive,
          't_facets__facet__terms__inactive__term': !vm.isActive,
        }"
        ng-click="vm.onClick()"
        style="padding: 0; position: relative;"
      >
          <ng-transclude></ng-transclude>
          <span
            class="term__extent-bar"
            ng-style="{ width: vm.itemsAffectedByTerm / vm.itemsAffectedByFacet * 100 + '%'}"
          ></span>
          <span
            class="term__label t_facets__facet__terms__inactive__term__label"
            data-tooltip="{{ vm.label }}"
            data-tooltip-placement="overflow"
          > 
            <i
              class="term__label__icon"
              ng-class="{
                'icon-ok': vm.isActive,
                'icon-check-empty': !vm.isActive,
              }"
            ></i>
            <span>{{ vm.label }}</span>
          </span>
          <span class="term__count t_facets__facet__terms__active__term__count">
            {{ vm.itemsAffectedByTerm }}
          </span>
      </div>
    `
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
                placeholder="e.g. Q9ULX7, ENSG00000144891"
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
                on-click="vm.toggleFacetContent('drugClass', 'fda')"
                is-active="vm.filters.drugClass.includes('fda')"
                label="'FDA'"
                items-affected-by-facet="vm.getFilteredCompounds(vm.compounds, _.omit(vm.filters, 'drugClass')).length"
                items-affected-by-term="_.filter(vm.getFilteredCompounds(vm.compounds, _.omit(vm.filters, 'drugClass')), {drugClass: 'fda'}).length"
              ></togglable-term>

              <togglable-term
                on-click="vm.toggleFacetContent('drugClass', 'world')"
                is-active="vm.filters.drugClass.includes('world')"
                label="'World'"
                items-affected-by-facet="vm.getFilteredCompounds(vm.compounds, _.omit(vm.filters, 'drugClass')).length"
                items-affected-by-term="_.filter(vm.getFilteredCompounds(vm.compounds, _.omit(vm.filters, 'drugClass')), {drugClass: 'world'}).length"
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
                  sort-column-id="vm.tableState.sortColumnId"
                  sort-order="vm.tableState.sortOrder"
                  items-per-page="vm.tableState.itemsPerPage"
                  current-page-number="vm.tableState.currentPageNumber"
                  columns="vm.columns"
                  sticky-header="true"
                  on-change="vm.handlePaginatedTableChange"
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
    controller: function (Page, CompoundIndexService, $location, $scope) {
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
        itemsPerPage: 10
      };

      this.filters = _.defaults(_.pick($location.search(), filterParams), defaultFiltersState);
      this.tableState = _.mapValues(_.defaults(_.pick($location.search(), paginatedTableParams), defaultTableState), (value, key) => _.isNumber(defaultTableState[key]) ? _.toNumber(value) : value);

      this.columns = [
        {
          id: 'name',
          heading: 'Name',
          isSortable: true,
          sortFunction: (row) => `${row.name} (${row.zincId})`,
          dataFormat: (cell, row, array) => {
            return `
              <a ui-sref="compound({compoundId: '${row.zincId}'})">
                ${row.name} (${row.zincId})
              </a>
            `;
          }
        },
        {
          id: 'atc',
          heading: 'ATC Level 4 Description',
          style: 'max-width: 200px',
          dataFormat: (cell, row, array) => {
            return `<div collapsible-text>
              ${_.map(row.atcCodes, 'description').join(', ')}
            </div>`;
          },
        },
        {
          id: 'compoundClass',
          heading: 'Compound Class',
          isSortable: true,
          field: 'drugClass',
          dataFormat: (cell, row, array) => {
            return { fda: 'FDA', world: 'World' }[cell];
          }
        },
        {
          id: 'geneCount',
          heading: '# Targed Genes',
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
            filters.gene ? (_.some(compound.genes, item => _.some(_.values(item).map(value => value.match(geneRegex))))) : true,
            filters.atc ? (_.some(compound.atcCodes, item => _.some(_.values(item).map(value => value.match(atcRegex))))) : true,
            filters.clinicalTrialCondition ? (_.some(_.flattenDeep(compound.trials.map(trial => trial.conditions.map(_.values))), str => str.match(clinicalTrialConditionRegex))) : true,
            filters.drugClass && filters.drugClass.length ? filters.drugClass.includes(compound.drugClass) : true,
          ]);
        });
      };

      // this.getFilteredAggregation = (filterName, filterValue) => {
      //   return _.filter(this.getFilteredCompounds(this.compounds, _.omit(this.filters, [filterName])), {[filterName]: filterValue}).length;
      // };

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
