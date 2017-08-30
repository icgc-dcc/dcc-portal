/*
 * Copyright 2016(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


  var module = angular.module('icgc.survival.services', []);

  module.service('SurvivalAnalysisService', function($q, Restangular, SetService) {

      function processResponses(responses) {
        var survivalData = responses.survivalData.plain().results;
        var setsMeta = responses.setsMeta.plain();

        var processGraphData = (graphType) => {
          return survivalData.map((dataSet) => {
            var donors = _.flatten(dataSet[graphType].map((interval) => {
              return interval.donors.map((donor) => {
                return _.extend({}, donor, {
                  survivalEstimate: interval.cumulativeSurvival
                });
              });
            }));

            return {
              meta: _.find(setsMeta, {id: dataSet.id}),
              donors: donors
            };
          });
        };
        var overallStats = isNaN(responses.survivalData.overallStats.pvalue) ? 
          undefined : responses.survivalData.overallStats;
        var diseaseFreeStats = isNaN(responses.survivalData.diseaseFreeStats.pvalue) ? 
          undefined : responses.survivalData.diseaseFreeStats;
        return {
          overall: processGraphData('overall'),
          overallStats: overallStats, 
          diseaseFree: processGraphData('diseaseFree'),
          diseaseFreeStats: diseaseFreeStats
        };
      }

      function fetchSurvivalData (setIds) {
        var data = setIds;

        var fetchSurvival = Restangular
          .one('analysis')
          .post('survival', data, {}, {'Content-Type': 'application/json'})
          .then((response) => Restangular.one('analysis/survival/' + response.id).get());

        var fetchSetsMeta = SetService.getMetaData(setIds);

        return $q.all({
            survivalData: fetchSurvival,
            setsMeta: fetchSetsMeta,
          })
          .then((responses) => processResponses(responses));
      }

      const defaultHeadingMap = {
        setName: 'donor_set_name',
        id: 'donor_id',
        time: 'time',
        status: 'donor_vital_status',
        survivalEstimate: 'survival_estimate',
      };

      function dataSetToTSV (dataSet, headingMap) {
        var headings = _({})
          .defaults(defaultHeadingMap, headingMap)
          .values()
          .join('\t');

        _.defaults({}, defaultHeadingMap, headingMap);

        var contents = _(dataSet)
          .map((set) => {
            return set.donors.map((donor) => {
              return [set.meta.name, donor.id, donor.time, donor.status, donor.survivalEstimate].join('\t');
            });
          })
          .flatten()
          .join('\n');

        var tsv = headings + '\n' + contents;
        return tsv;
      }

      _.extend(this, {
        fetchSurvivalData,
        dataSetToTSV
      });

    });

  module.service('CohortAnalysisCreationService', function (
    SetService,
    SetNameService,
    LocationService,
    Restangular
  ) {

    const createComplementarySet = (originalSetId, subsetId, { name }) => SetService.addDerivedSet('DONOR', {
      name,
      union: [{
        intersection: [ originalSetId ],
        exclusions: [ subsetId ],
      }],
    });

    const bisectDonorSet = async (donorSetId, bisectingFilter) => {
      const filterName = await SetNameService.getSetName(bisectingFilter);
      const setA = await SetService.addSet('donor', {
        name: `Set with ${filterName}`,
        filters: _.merge({donor: {id: {is: [`ES:${donorSetId}`]}}}, bisectingFilter),
        isTransient: true,
        type: 'donor',
      });
      const setB = await createComplementarySet(donorSetId, setA.id, { name: `Set without ${filterName}`});
      return [setA, setB];
    };

    const bisectDonorSetAndRunAnalysis = (donorSetId, bisectingFilter) => 
      bisectDonorSet(donorSetId, bisectingFilter)
        .then(([setA, setB]) => launchCohortAnalysis([setA.id, setB.id]));

    const launchCohortAnalysis = (setIds) =>
      Restangular
        .one('analysis')
        .post('phenotype', setIds, {}, {'Content-Type': 'application/json'})
        .then((data) => {
          if (!data.id) {
            throw new Error('Could not retrieve analysis data id', data);
          }
          LocationService.goToPath('analysis/view/phenotype/' + data.id);
        });

    _.extend(this, {
      bisectDonorSet,
      bisectDonorSetAndRunAnalysis,
    });

  });

  module.service('SurvivalAnalysisLaunchService', function(SetService, Restangular, LocationService,
    SetNameService, $timeout, $location, Page){
      const _service = this;
      _service.setName = '';

      const updateSetName = () => {
        return SetNameService.getSetFilters()
          .then((filters) => SetNameService.getSetName(filters))
          .then((setName) => {_service.setName = setName});
      };

      // Wait for sets to materialize
      const wait = (ids, numTries, callback) => {
        if (numTries <= 0) {
          return;
        }
        SetService.getMetaData(ids).then((data) => {
          var finished = _.filter(data, (d) =>  d.state === 'FINISHED');

          if (finished.length === ids.length) {
            callback(data);
          } else {
            $timeout(() => {
              wait(ids, --numTries, callback);
            }, 1500);
          }
        });
      };

       /* Phenotype comparison only takes in donor set ids */
      const launchAnalysis = (setIds) => {
        return Restangular
          .one('analysis')
          .post('phenotype', setIds, {}, {'Content-Type': 'application/json'})
          .then((data) => {
            if (!data.id) {
              throw new Error('Could not retrieve analysis data id', data);
            }
            LocationService.goToPath('analysis/view/phenotype/' + data.id);
          }).finally(() => {
            Page.stopWork();
          });
      };

      const getSetObject = (setId, entityType, entityId, filterType) => ({
        filters: _.merge({donor: {id: {is: [`ES:${setId}`]}}}, {[entityType]: { id: { [filterType]: [entityId] } }}),
        isTransient: true,
        type: 'donor',
      });

      _service.launchSurvivalAnalysis = async (entityType, entityId, entitySymbol, filters, projectId) => {

        const isGene = _.isEqual(entityType, 'gene'),
          type = 'donor',
          projectCode = projectId || '';

        /* FIXME: This will be a workaround since currently we dont support
        ** IS and NOT in the filters query for the same entity type.
        ** For the workaround we are first creating a dummy donor set(initialSet)
        ** without the entity ID(entityId) we are runing analysis on,
        ** therefore the _.remove method.
        ** Once given the set ID for the dummy set, we then create two sets.
        ** One containing IS filter and the other one with NOT filter.
        ** This will make sure that we get correct data to run analysis on.
        **/

        _.remove(_.get(filters, `${entityType}.id.is`), entityId);

        let initialDonorSet, donorSet1, donorSet2;

        initialDonorSet = {
          filters,
          isTransient: true,
          type,
          name: 'dummy Donor Set'
        };

        const initialSet = await SetService.addSet(type, initialDonorSet);

        Page.startWork();

        await updateSetName();

        _service.setName = _.includes(_service.setName, 'All') ? '' : _service.setName;

        donorSet1 = {
          ...getSetObject(initialSet.id, entityType, entityId, 'is'),
          name: isGene ? `${entitySymbol} Mutated Donors ${projectCode} ${_service.setName}` : `Donors with mutation ${entitySymbol} ${projectCode} ${_service.setName}`
        };

        donorSet2 = {
          ...getSetObject(initialSet.id, entityType, entityId, 'not'),
          name: isGene ? `${entitySymbol} Not Mutated Donors ${projectCode} ${_service.setName}` : `Donors without mutation ${entitySymbol} ${projectCode} ${_service.setName}`
        };

        const sets = [await SetService.addSet(type, donorSet1), await SetService.addSet(type, donorSet2)];
        const setIds = sets.map(set => set.id);
        wait(setIds, 7, () => launchAnalysis(setIds));
      };

    });
