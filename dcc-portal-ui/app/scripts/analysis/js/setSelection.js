angular.module('icgc.analysis.setSelection', [])
  .component('setSelection', {
    template: `
      <div><em>{{vm.description}}</em></div>
      <div>Demo: <a href="" ng-click="vm.onClickDemo()">{{vm.demoCtaText}}</a></div>
      <br>
      <div> 
          <div ng-bind-html="vm.selectionInstructions"></div>
          <table class="table table-selectable"> 
              <thead>
                  <th></th>
                  <th><translate>Item Type</translate></th>
                  <th><translate>Name</translate></th>
                  <th class="text-right"><translate># Items</translate></th>
              </thead>
              <tbody>
                  <tr ng-if="!vm.sets.length">
                      <td colspan="5" class="text-center"><strong><translate>No saved sets</translate></strong></td>
                  </tr>
                  <tr
                    ng-repeat="item in (vm.sets | _:'sortBy':vm.isSetCompatibleWithAnalysis).reverse()"
                    style="color: {{
                      vm.isSetCompatibleWithAnalysis(item)
                        ? vm.isSetCompatibleWithSelectedSets(item)
                          ? ''
                          : '#777'
                        : '#CCC'
                    }}"
                    ng-click="vm.isSetCompatibleWithAllContexts(item) && vm.handleClickItem(item)"
                  > 
                      <td class="text-center" style="width:2rem"> 
                          <span
                            ng-if="vm.isSetCompatibleWithAllContexts(item)"
                          >
                            <i ng-class="{
                              'icon-check-empty': vm.selectedSets.indexOf(item) === -1,
                              'icon-check': vm.selectedSets.indexOf(item) >= 0,
                            }"></i>
                          </span>
                      </td>
                      <td>{{item.type | readable}}</td>
                      <td tooltip="{{vm.getSetCompatibilityMessage(item)}}">{{item.name}}</td>
                      <td class="text-right"> 
                          <a href="{{item.advLink}}">{{item.count | number}}</a>
                      </td>
                  </tr>
              </tbody>
          </table>
          <br>
          <button
            class="t_button"
            ng-disabled="vm.shouldDisableRunButton()"
            ng-click="vm.onClickLaunch(vm.selectedSets)"
          >
              <span ng-if="vm.isLaunchingAnalysis"><i class="icon-spinner icon-spin"></i></span>
              <translate>Run</translate>
          </button>
      </div>
    `,
    bindings: {
      sets: '<',
      description: '<',
      demoCtaText: '<',
      onClickDemo: '&',
      selectionInstructions: '<',
      isLaunchingAnalysis: '<',
      setCompatibilityCriteria: '<',
      onClickLaunch: '&',
      areSetsValid: '&',
    },
    controller: function ($scope, $element) {
      this.selectedSets = [];
      this.shouldDisableRunButton = () => this.areSetsValid(this.selectedSets) || this.isLaunchingAnalysis;
      this.handleClickItem = item => { this.selectedSets = _.xor(this.selectedSets, [item]) };

      this.isSetCompatibleWithContext = (set, context) => _.every(_.filter(this.setCompatibilityCriteria, {context}), criterium => criterium.test(set, this.selectedSets));
      this.isSetCompatibleWithAnalysis = set => this.isSetCompatibleWithContext(set, 'ANALYSIS');
      this.isSetCompatibleWithSelectedSets = set => this.isSetCompatibleWithContext(set, 'SELECTED_SETS');
      this.isSetCompatibleWithAllContexts = (set) => this.isSetCompatibleWithAnalysis(set) && this.isSetCompatibleWithSelectedSets(set);
      this.getSetCompatibilityMessage = (set) => this.setCompatibilityCriteria
            .filter(criterium => !criterium.test(set, this.selectedSets))
            .map(x => x.message)
            .join(', ');
    },
    controllerAs: 'vm',
  })
