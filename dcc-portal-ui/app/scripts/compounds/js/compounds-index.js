import _ from 'lodash';

angular.module('icgc.compounds.index', [])
  .config(function ($stateProvider) {
    $stateProvider.state('compound-index', {
      url: '/compounds?name&gene&atc&clinicalTrialCondition&drugClass',
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
              <div
                ng-class="{
                  't_facets__facet__terms__active__term__label': vm.filters.drugClass.includes('fda'),
                  't_facets__facet__terms__inactive__term': !vm.filters.drugClass.includes('fda'),
                }"
                data-ng-click="vm.toggleFacetContent('drugClass', 'fda')"
                style="padding: 0;"
              >
                  <span
                    class="t_facets__facet__terms__inactive__term__label"
                    data-tooltip="FDA"
                    data-tooltip-placement="overflow"
                  >
                    <i
                      ng-class="{
                        'icon-ok': vm.filters.drugClass.includes('fda'),
                        'icon-check-empty': !vm.filters.drugClass.includes('fda'),
                      }"
                    ></i>
                    <translate>
                      <span>FDA</span>
                    </translate>
                  </span>
             </div>

             <div
                ng-class="{
                  't_facets__facet__terms__active__term__label': vm.filters.drugClass.includes('world'),
                  't_facets__facet__terms__inactive__term': !vm.filters.drugClass.includes('world'),
                }"
                data-ng-click="vm.toggleFacetContent('drugClass', 'world')"
                style="padding: 0;"
              >
                  <span
                    class="t_facets__facet__terms__inactive__term__label"
                    data-tooltip="World"
                    data-tooltip-placement="overflow"
                  >
                    <i
                      ng-class="{
                        'icon-ok': vm.filters.drugClass.includes('world'),
                        'icon-check-empty': !vm.filters.drugClass.includes('world'),
                      }"
                    ></i>
                    <translate>
                      <span>World</span>
                    </translate>
                  </span>
             </div>

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
                  initial-sort-column-id="'geneCount'"
                  initial-sort-order="'desc'"
                  columns="vm.columns"
                  sticky-header="true"
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
    controller: function (Page, CompoundIndexService, $location) {
      Page.setTitle('Compounds');
      Page.setPage('entity');

      this.isLoading = false;

      const update = _.debounce(async () => {
        this.isLoading = true;
        this.compounds = await CompoundIndexService.getAll();
        this.isLoading = false;
        this.handleFiltersChange(this.filters);
      });

      this.$onInit = update;
      this.$onChanges = update;

      this.filters = $location.search() || {};
      console.log('filters was ', this.filters);

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

      this.handleFiltersChange = (filters) => {
        const nameRegex = new RegExp(this.filters.name, 'i');
        const atcRegex = new RegExp(this.filters.atc, 'i');
        const geneRegex = new RegExp(this.filters.gene, 'i');
        const clinicalTrialConditionRegex = new RegExp(this.filters.clinicalTrialCondition, 'i');

        this.filteredCompounds = _.filter(this.compounds, (compound) => {
          return _.every([
            this.filters.name ? (compound.name.match(nameRegex) || compound.zincId.match(nameRegex)) : true,
            this.filters.gene ? (_.some(compound.genes, item => _.some(_.values(item).map(value => value.match(geneRegex))))) : true,
            this.filters.atc ? (_.some(compound.atcCodes, item => _.some(_.values(item).map(value => value.match(atcRegex))))) : true,
            this.filters.clinicalTrialCondition ? (_.some(_.flattenDeep(compound.trials.map(trial => trial.conditions.map(_.values))), str => str.match(clinicalTrialConditionRegex))) : true,
            this.filters.drugClass && this.filters.drugClass.length ? this.filters.drugClass.includes(compound.drugClass) : true,
          ]);
        });

        $location.replace();
        $location.search(this.filters);
      };
    },
    controllerAs: 'vm',
  })
  ;
