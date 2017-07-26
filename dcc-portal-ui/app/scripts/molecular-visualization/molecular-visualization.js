import getPdbIds from './getPdbIds';

require('./molecular-visualization.scss');

angular.module('icgc.molecular.visualization', [])
  .component('molecularVisualization', {
    template: `
      <div class="loading-message" ng-show="true || vm.loadState.isLoading">
        ...
      </div>
      <div class="viewport" style="width: 100%; height: 400px;"></div>
    `,
    bindings: {
      uniprotId: '<',
    },
    controller: function ($scope, $element, LoadState) {
      var loadState = new LoadState();
      this.loadState = loadState;
      require.ensure([], (require) => {
        const NGL = require('ngl');
        var stage = new NGL.Stage($element.find('.viewport').get(0), {
          backgroundColor: 'white',
        });
        const uniprotId = this.uniprotId[0];
        loadState.loadWhile(
          getPdbIds(uniprotId)
            .then(async (pdbIds) => {
              if (pdbIds[0]) {
                await stage.loadFile(`rcsb://${pdbIds[0]}`, {
                  defaultRepresentation: true,
                });
                stage.setSpin(true);
              } else {
                console.log(`No pdb id found for ${uniprotId}`);
              }
            })
        );
        this.$onDestroy = () => stage.dispose();
      });
    },
    controllerAs: 'vm',
  });
