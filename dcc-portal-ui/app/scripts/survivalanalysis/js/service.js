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

(function(){
  'use strict'

  var module = angular.module('icgc.survival.services', []);

  module.service('SurvivalAnalysisService', function($q, Restangular, SetService) {

      function processResponses(responses) {
        var survivalData = responses.survivalData.plain().results;
        var setsMeta = responses.setsMeta.plain();

        var processGraphData = function (graphType) {
          return survivalData.map(function (dataSet) {
            var donors = _.flatten(dataSet[graphType].map(function (interval) {
              return interval.donors.map(function (donor) {
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
          .then(function (response) {
            return Restangular.one('analysis/survival/' + response.id).get();
          });

        var fetchSetsMeta = SetService.getMetaData(setIds);

        return $q.all({
          survivalData: fetchSurvival,
          setsMeta: fetchSetsMeta,
        })
          .then(function (responses) {
            return processResponses(responses);
          });
      }

      var defaultHeadingMap = {
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
          .map(function (set) {
            return set.donors.map(function (donor) {
              return [set.meta.name, donor.id, donor.time, donor.status, donor.survivalEstimate].join('\t');
            });
          })
          .flatten()
          .join('\n');

        var tsv = headings + '\n' + contents;
        return tsv;
      }

      _.extend(this, {
        fetchSurvivalData: fetchSurvivalData,
        dataSetToTSV: dataSetToTSV
      });

    });

  module.service('SurvivalAnalysisLaunchService', function(SetService, Restangular, LocationService,
    SetNameService, $timeout, $location, Page){
      const _service = this;

      var d1, d2, type = 'donor';

      _service.getSetName = function(filters){
        return SetNameService.getSetFilters()
          .then(function (filters) {
            return SetNameService.getSetName(filters);
          });
      }

      // Wait for sets to materialize
      _service.wait = (ids, numTries, callback) => {
        if (numTries <= 0) {
          return;
        }
        SetService.getMetaData(ids).then(function(data) {
          var finished = _.filter(data, function(d) {
            return d.state === 'FINISHED';
          });

          if (finished.length === ids.length) {
            callback(data);
          } else {
            $timeout(function() {
              _service.wait(ids, --numTries, callback);
            }, 1500);
          }
        });
      }

       /* Phenotype comparison only takes in donor set ids */
      _service.launchAnalysis = (setIds) => {
        return Restangular
          .one('analysis')
          .post('phenotype', setIds, {}, {'Content-Type': 'application/json'})
          .then(function(data) {
            if (!data.id) {
              console.log('Could not retrieve analysis data.id');
            }
            LocationService.goToPath('analysis/view/phenotype/' + data.id);
          }).finally(function(){
            Page.stopWork();
          });
      };

      _service.launchSurvivalAnalysis = (entityType, entityId, entitySymbol, filters) => {

        Page.startWork();

        _service.getSetName(filters).then(function (setName) {
          d1 = {
            filters: entityType === 'gene' ? _.extend(_.clone(filters), {gene: { id: { is: [entityId] } }}) : _.extend(_.clone(filters), {mutation: { id: { is: [entityId] } }}),
            isTransient: true,
            type: type,
            name: entityType === 'gene' ? entitySymbol + ' Mutated Donors ' + setName : 'Donors with ' + entitySymbol + ' ' + setName
          };

          d2 = {
            filters: entityType === 'gene' ? _.extend(_.clone(filters), {gene: { id: { not: [entityId] } }}) : _.extend(_.clone(filters), {mutation: { id: { not: [entityId] } }}),
            isTransient: true,
            type: type,
            name: entityType === 'gene' ? entitySymbol + ' Not Mutated Donors ' + setName : 'Donors without ' + entitySymbol + ' ' + setName
          };

          var setIds = [];

          SetService.addSet(type, d1).then(function (r1) {
            setIds.push(r1.id);
            SetService.addSet(type, d2).then(function (r2) {
              setIds.push(r2.id);
              function proxyLaunch() {
                _service.launchAnalysis(setIds);
              }
              _service.wait(setIds, 7, proxyLaunch);
            });
          });
        });

      }

    });

})();