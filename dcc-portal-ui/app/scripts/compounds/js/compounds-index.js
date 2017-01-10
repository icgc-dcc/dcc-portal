angular.module('icgc.compounds.index', [])
  .config(function ($stateProvider) {
    $stateProvider.state('compound-index', {
      url: '/compound',
      template: '<compound-index></compound-index>',
      reloadOnSearch: false,
    });
  })
  .service('CompoundIndexService', function (Restangular) {
    const getAll = () => Restangular.all('drugs').getList().then(Restangular.stripRestangular);

    Object.assign(this, {
      getAll,
    });
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
        <pre>
          {{ vm.compounds | json }}
        </pre>
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
