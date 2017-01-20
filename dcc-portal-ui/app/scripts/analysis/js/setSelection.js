require('./setTools');

angular.module('icgc.analysis.setSelection', [])
  .component('setSelection', {
    template: require('../views/set-selection.html'),
    bindings: {
      sets: '<',
      description: '<',
      demoCtaText: '<',
      selectionInstructions: '<',
      isLaunchingAnalysis: '<',
      analysisSatisfactionCriteria: '<',
      onClickCancel: '&',
      onClickLaunch: '&',
      onClickLaunchDemo: '&',
      onSelectedSetsChange: '&',
    },
    controller: function (
      $scope,
      $element,
      SetService,
      $window,
      gettextCatalog
    ) {

      const prepareCallbacks = () => {
        this.handleClickCancel = () => {
          this.setSelectedSets([]);
          this.onClickCancel();
        };
        this.handleClickLaunch = this.onClickLaunch();
        this.handleClickLaunchDemo = this.onClickLaunchDemo();
        this.handleSelectedSetsChange = this.onSelectedSetsChange();
      };

      this.selectedSets = [];

      this.setSelectedSets = (sets) => {
        this.selectedSets = sets;
        this.handleSelectedSetsChange(this.selectedSets);
      };

      this.deleteSets = (sets) => {
        if (!$window.confirm(gettextCatalog.getString('Are you sure you want to remove selected sets?'))) {
          return;
        }

        if (sets.length > 0) {
          SetService.removeSeveral(_.map(sets, 'id'));
          this.setSelectedSets([]);
        }
      };

      this.areAllSetsSelected = () => !_.xor(this.selectedSets, this.sets).length;

      this.isSetSelected = set => _.includes(this.selectedSets, set);
      this.isAnalysisRunnable = () => this.hasAnalysis() && this.isAnalysisSatisfied() && !this.isLaunchingAnalysis;
      this.handleClickItem = item => this.setSelectedSets(_.xor(this.selectedSets, [item]));

      const doSetsSatsifyCriteria =  (sets, criteria) => _.every(criteria || [], criterium => criterium.test(sets));
      this.isSetCompatible = set => doSetsSatsifyCriteria(_.uniq(this.selectedSets.concat(set)), _.reject(this.analysisSatisfactionCriteria, {type: 'MIN_SETS'}));
      this.isSetCompatibleWithAnalysis = set => doSetsSatsifyCriteria([set], _.reject(this.analysisSatisfactionCriteria, {type: 'MIN_SETS'}));

      this.hasAnalysis = () => this.analysisSatisfactionCriteria && this.analysisSatisfactionCriteria.length;
      this.isAnalysisSatisfied = () => doSetsSatsifyCriteria(this.selectedSets, this.analysisSatisfactionCriteria);
      const getCriteriaSatisfactionMessages = (sets, criteria) => _.reject(criteria || [], criterium => criterium.test(sets)).map(criteria => criteria.message);
      this.getSetCompatibilityMessage = set => getCriteriaSatisfactionMessages(_.uniq(this.selectedSets.concat(set)), _.reject(this.analysisSatisfactionCriteria, {type: 'MIN_SETS'})).join('<br>');
      this.getAnalysisSatifactionMessage = () => getCriteriaSatisfactionMessages(this.selectedSets, this.analysisSatisfactionCriteria).join(', ');

      this.handleSaveSetName = (set, newName) => SetService.renameSet(vm.set.id, newName);

      this.$onChanges = changes => prepareCallbacks();
      this.$onInit = () => prepareCallbacks();
    },
    controllerAs: 'vm',
  });
