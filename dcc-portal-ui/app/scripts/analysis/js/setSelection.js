angular.module('icgc.analysis.setSelection', [])
  .component('setSelection', {
    template: require('../views/set-selection.html'),
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
