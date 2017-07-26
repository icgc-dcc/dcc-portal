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
      var loadState = new LoadState();
      this.loadState = loadState;
      require.ensure([], (require) => {
        const NGL = require('ngl');
        var stage = new NGL.Stage($element.find('.viewport').get(0), {
          backgroundColor: 'white',
        });

        stage.spinAnimation.angle = 0;
        stage.setSpin(true);

        const uniprotId = this.uniprotId[0];
        loadState.loadWhile(
          getPdbIds(uniprotId)
            .then(async (pdbIds) => {
              if (pdbIds[0]) {
                await stage.loadFile(`rcsb://${pdbIds[0]}`, {
                  defaultRepresentation: true,
                });
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
