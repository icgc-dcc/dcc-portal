angular.module('icgc.analysis.setSelection', [])
  .component('setSelection', {
    template: `
      <div class="set-selection-component">
      <div><em>{{vm.description}}</em></div>
      <div>Demo: <a href="" ng-click="vm.onClickLaunchDemo()">{{vm.demoCtaText}}</a></div>
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
                      <td colspan="5" class="text-center">
                        <strong><translate>No saved sets</translate></strong>
                      </td>
                  </tr>
                  <tr
                    ng-repeat="item in (
                        vm.sets
                          | _:'filter':vm.isSetCompatibleWithAnalysis
                          | _:'sortBy':'timestamp'
                      ).reverse().concat((
                        vm.sets
                          | _:'reject':vm.isSetCompatibleWithAnalysis
                          | _:'sortBy':'timestamp'
                          ).reverse()
                      )"
                    style="color: {{
                      vm.isSetCompatibleWithAnalysis(item)
                        ? (vm.isSetSelected(item) || vm.isSetCompatibleWithSelectedSets(item))
                          ? ''
                          : '#777'
                        : '#CCC'
                    }}"
                    ng-class="{'is-selected': vm.isSetSelected(item)}"
                  > 
                      <td
                        class="text-center"
                        ng-click="
                          (vm.isSetCompatibleWithAllContexts(item) || vm.isSetSelected(item))
                            && vm.handleClickItem(item)
                          "
                        tooltip="{{vm.getSetCompatibilityMessage(item)}}"
                        tooltip-placement="right"
                        style="
                          width: 2rem;
                          cursor: {{ (vm.isSetCompatibleWithAllContexts(item) || vm.isSetSelected(item)) ? 'pointer' : 'help' }}
                        "
                      > 
                            <i
                              ng-if="vm.isSetCompatibleWithAnalysis(item)"
                              ng-class="{
                              'icon-check-empty': !vm.isSetSelected(item),
                              'icon-check': vm.isSetSelected(item),
                              }"
                            ></i>

                            <i
                              ng-if="!vm.isSetCompatibleWithAnalysis(item)"
                              class="icon-info"
                            ></i>
                      </td>
                      <td>{{item.type | readable}}</td>
                      <td
                        class="set-name-wrapper"
                      >
                        <span
                            ng-click="itemNameForm.$show()"
                            ng-hide="itemNameForm.$visible"
                            editable-text="item.name"
                            e-form="itemNameForm"
                            onaftersave="vm.handleSaveSetName(item, item.name)"
                        >
                            {{ item.name }}
                        </span>
                        <i
                            tooltip="{{'Rename set' | translate}}"
                            class="icon-pencil"
                            ng-click="itemNameForm.$show()"
                            ng-hide="itemNameForm.$visible"
                        ></i>
                      </td>
                      <td class="text-right"> 
                          <a href="{{item.advLink}}">{{item.count | number}}</a>
                      </td>
                  </tr>
              </tbody>
          </table>
          <br>
          <div
            tooltip="{{vm.getAnalysisSatifactionMessage()}}"
            tooltip-placement="right"
          >
            <button
              class="t_button"
              ng-disabled="!vm.isAnalysisRunnable()"
              ng-click="vm.onClickLaunch(vm.selectedSets)"
            >
                <span ng-if="vm.isLaunchingAnalysis"><i class="icon-spinner icon-spin"></i></span>
                <translate>Run</translate>
            </button>
          </span>
      </div>
      </div>
    `,
    bindings: {
      sets: '<',
      description: '<',
      demoCtaText: '<',
      selectionInstructions: '<',
      isLaunchingAnalysis: '<',
      setCompatibilityCriteria: '<',
      analysisSatisfactionCriteria: '<',
      onClickLaunch: '&',
      onClickLaunchDemo: '&',
    },
    controller: function ($scope, $element, SetService) {
      this.onClickLaunch = this.onClickLaunch();
      this.onClickLaunchDemo = this.onClickLaunchDemo();

      this.selectedSets = [];
      this.isSetSelected = set => _.includes(this.selectedSets, set);
      this.isAnalysisRunnable = () => this.isAnalysisSatisfied() && !this.isLaunchingAnalysis;
      this.handleClickItem = item => { this.selectedSets = _.xor(this.selectedSets, [item]) };

      this.isSetCompatibleWithContext = (set, context) => _.every(_.filter(this.setCompatibilityCriteria, {context}), criterium => criterium.test(set, this.selectedSets));
      this.isSetCompatibleWithAnalysis = set => this.isSetCompatibleWithContext(set, 'ANALYSIS');
      this.isSetCompatibleWithSelectedSets = set => this.isSetCompatibleWithContext(set, 'SELECTED_SETS');
      this.isSetCompatibleWithAllContexts = (set) => this.isSetCompatibleWithAnalysis(set) && this.isSetCompatibleWithSelectedSets(set);

      this.isAnalysisSatisfied = () => _.every(this.analysisSatisfactionCriteria, criterium => criterium.test(this.selectedSets));
      
      const getUnsatisfiedCriteriaMessage = (criteria, testingFn, separator = ', ') => criteria
        .filter(testingFn)
        .map(x => x.message)
        .join(separator) 

      this.getSetCompatibilityMessage = (set) => this.isSetSelected(set) ? '' : getUnsatisfiedCriteriaMessage(this.setCompatibilityCriteria, criterium => !criterium.test(set, this.selectedSets));
      this.getAnalysisSatifactionMessage = () => getUnsatisfiedCriteriaMessage(this.analysisSatisfactionCriteria, criterium => !criterium.test(this.selectedSets));

      this.handleSaveSetName = (set, newName) => SetService.renameSet(set.id, newName);
    },
    controllerAs: 'vm',
  })
