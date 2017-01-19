import _ from 'lodash';

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
          class="t_facets__facet__input"
          ng-transclude
          ng-show="!vm.isCollapsed"
        >
          <input
            class="t_input__block"
            type="search"
            ng-model="vm.filters.name"
            ng-change="vm.handleFiltersChange(vm.filters)"
            placeholder="e.g. Aspirin, ZINC00003376543"
          >
        </li>
      </ul>
    `
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
                placeholder="e.g. BRAF"
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
              <label>
                <input
                  class="t_input__block"
                  type="checkbox"
                  ng-click="vm.toggleFacetContent('drugClass', 'fda')"
                ></input>
                FDA
              </label>

              <label>
                <input
                  class="t_input__block"
                  type="checkbox"
                  ng-click="vm.toggleFacetContent('drugClass', 'world')"
                ></input>
                World
              </label>
            </collapsible-wrapper>
          </aside>
          <article>
            <section>
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
            </section>
          </article>
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
          isSortable: true,
          field: 'drugClass',
          dataFormat: (cell, row, array) => {
            return { fda: 'FDA', world: 'World' }[cell];
          }
        },
        {
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
        console.log('check change');
        this.filters[facet] = _.xor((this.filters[facet] || []), [classType]);
        this.handleFiltersChange(this.filters);
      };

      this.handleFiltersChange = (filters) => {
        const nameRegex = new RegExp(this.filters.name, 'i');
        const atcRegex = new RegExp(this.filters.atc, 'i');
        const clinicalTrialConditionRegex = new RegExp(this.filters.clinicalTrialCondition, 'i');
        console.log(this.filters.class);
        this.filteredCompounds = _.filter(this.compounds, (compound) => {
          return _.every([
            this.filters.name ? (compound.name.match(nameRegex) || compound.zincId.match(nameRegex)) : true,
            this.filters.atc ? (_.some(compound.atcCodes, item => _.some(_.values(item).map(value => value.match(atcRegex))))) : true,
            this.filters.clinicalTrialCondition ? (_.some(_.flattenDeep(compound.trials.map(trial => trial.conditions.map(_.values))), str => str.match(clinicalTrialConditionRegex))) : true,
            this.filters.drugClass && this.filters.drugClass.length ? this.filters.drugClass.includes(compound.drugClass) : true,
          ]);
        });
      }
    },
    controllerAs: 'vm',
  })
  ;
