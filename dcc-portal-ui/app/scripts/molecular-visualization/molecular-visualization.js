import getPdbIds from './getPdbIds';

require('./molecular-visualization.scss');

angular.module('icgc.molecular.visualization', [])
  .component('molecularVisualization', {
    template: `
      <div class="loading-message" ng-show="vm.loadState.isLoading">
        <i>.</i><i>.</i><i>.</i><i>.</i>
      </div>
      <div class="not-found-message" ng-if="vm.hasNoResult">
        No matching structure found
      </div>
      <div ng-if="vm.pdbIds.length">
        <select
          ng-options="id for id in vm.pdbIds"
          ng-model="vm.pdbIdOnDisplay"
          ng-change="vm.handleChangePdbId(vm.pdbIdOnDisplay)"
        >
        </select>
      </div>
      <div
        class="viewport"
        style="width: 100%; height: 400px;"
        ng-mouseenter="vm.onMouseEnter()"
        ng-mouseleave="vm.onMouseLeave()"
      ></div>
    `,
    bindings: {
      uniprotId: '<',
    },
    controller: function ($scope, $element, LoadState, $timeout) {
      const loadState = new LoadState();
      this.loadState = loadState;
      this.pdbIds = [];
      this.pdbIdOnDisplay;
      let currentStructure;

      require.ensure([], (require) => {
        const NGL = require('ngl');
        let stage = new NGL.Stage($element.find('.viewport').get(0), {
          backgroundColor: 'white',
        });

        stage.spinAnimation.angle = 0;
        stage.setSpin(true);

        const loadStructure = async (pdbId) => {
          this.pdbIdOnDisplay = pdbId;
          stage.setSpin(false);
          currentStructure && stage.removeComponent(currentStructure);
          currentStructure = await stage.loadFile(`rcsb://${pdbId}`, {
            defaultRepresentation: true,
          });
          startSpin();
          stage.autoView(1);
        };

        this.handleChangePdbId = (pdbId) => {
          loadStructure(pdbId);
        };

        const uniprotId = this.uniprotId[0];
        loadState.loadWhile(
          getPdbIds(uniprotId)
            .then(async (pdbIds) => {
              if (pdbIds[0]) {
                this.pdbIds = pdbIds;
                await loadStructure(pdbIds[0]);
                startSpin();
              } else {
                this.hasNoResult = true;
              }
            })
        );

        this.$onDestroy = () => stage.dispose();

        const tweenr = require('tweenr')();
        const startSpin = () => {
          stage.setSpin(true);
          const tween = tweenr.to(
            { angle: 0 },
            { angle: 0.001, duration: 1, ease: 'sineIn' });
          tween.on('update', (tween) => {
            stage.spinAnimation.angle = tween.target.angle;
          });
        };

        const stopSpin = () => {
          const tween = tweenr.to(
            { angle: 0.001 },
            { angle: 0, duration: 0.4, ease: 'expoOut' });
          tween.on('update', (tween) => {
            stage.spinAnimation.angle = tween.target.angle;
          });
          tween.on('complete', () => stage.setSpin(false));
        };

        let interactionTimeout;
        this.onMouseEnter = () => {
          $timeout.cancel(interactionTimeout);
          stopSpin();
        };
        this.onMouseLeave = () => {
          interactionTimeout = $timeout(startSpin, 0.3 * 1000);
        };
      });

    },
    controllerAs: 'vm',
  });
