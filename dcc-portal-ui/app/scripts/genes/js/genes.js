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

(function () {
  'use strict';

  var module = angular.module('icgc.genes', ['icgc.genes.controllers', 'ui.router']);

  module.config(function ($stateProvider) {
    $stateProvider.state('gene', {
      url: '/genes/:id',
      templateUrl: 'scripts/genes/views/gene.html',
      controller: 'GeneCtrl as GeneCtrl',
      resolve: {
        gene: ['$stateParams', 'Genes', function ($stateParams, Genes) {
          return Genes.one($stateParams.id).get({include: ['projects', 'transcripts']}).then(function(gene) {
            return gene;
          });
        }]
      }
    });
  });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.genes.controllers', ['icgc.genes.models']);

  module.controller('GeneCtrl', function ($scope, HighchartsService, Page, Projects, Mutations,
    LocationService, Donors, Genes, GMService, Restangular, ExternalLinks, gene) {

    var _ctrl = this;
    Page.setTitle(gene.id);
    Page.setPage('entity');

    _ctrl.ExternalLinks = ExternalLinks;

    _ctrl.gvOptions = {location: false, panels: false, zoom: 50};


    _ctrl.gene = gene;
    _ctrl.gene.uiProteinTranscript = [];
    _ctrl.gene.fprojects = [];
    _ctrl.totalDonors = 0;
    _ctrl.gene.hasGVChromosome = GMService.isValidChromosome(_ctrl.gene.chromosome);

    function extractAndSort(list, type) {
      var filtered =  _.filter(list, function(set) {
        return set.type === type && set.annotation === 'direct';
      });
      return _.sortBy(filtered, function(set) {
        return set.name;
      });
    }

    // Extract to make it easier to handle on the view
    _ctrl.uiGeneSets = {};
    _ctrl.uiGeneSets.pathwayList = extractAndSort(_ctrl.gene.sets, 'pathway');
    _ctrl.uiGeneSets.goList = extractAndSort(_ctrl.gene.sets, 'go_term');
    _ctrl.uiGeneSets.curatedList = extractAndSort(_ctrl.gene.sets, 'curated_set');


    function refresh() {
      var geneProjectPromise = Donors.getList({
        size: 0,
        from: 1,
        include: ['facets'],
        filters: {'gene':{'id':{'is':[_ctrl.gene.id]}}}
      }).then(function(data) {
        var ids = _.pluck(data.facets.projectId.terms, 'term');

        if (_.isEmpty(ids)) {
          return [];
        }

        return Projects.getList({
          filters: {'project': {'id': { 'is': ids}}}
        });
      });

      _ctrl.gene.advQuery = LocationService.mergeIntoFilters({gene: {id: {is: [_ctrl.gene.id] }}});

      // Fetch dynaimc mutations and donors
      geneProjectPromise.then(function(projects) {
        var mutationPromise, donorPromise;
        if ( !projects.hits || projects.hits.length === 0) {
          _ctrl.gene.projects = [];
          return;
        }

        mutationPromise = Projects.one(_.pluck(projects.hits, 'id').join(','))
          .handler.one('mutations', 'counts').get({filters: _ctrl.gene.advQuery });

        donorPromise = Projects.one(_.pluck(projects.hits, 'id').join(','))
          .handler.one('donors', 'counts').get({filters: _ctrl.gene.advQuery });

        mutationPromise.then(function(projectMutations) {
          projects.hits.forEach(function(proj) {
            proj.mutationCount = projectMutations[proj.id];
          });
          _ctrl.totalMutations = projectMutations.Total;
        });

        donorPromise.then(function(projectDonors) {
          projects.hits.forEach(function(proj) {
            proj.filteredDonorCount = projectDonors[proj.id];
            proj.uiAffectedDonorPercentage = proj.filteredDonorCount / proj.ssmTestedDonorCount;
            proj.advQuery = LocationService.mergeIntoFilters({
              gene: {id: {is: [_ctrl.gene.id] }},
              donor: {projectId: {is: [proj.id]}}
            });
          });
          _ctrl.bar = HighchartsService.bar({
            hits: _.take(_.sortBy(projects.hits, function (p) {
              return -p.uiAffectedDonorPercentage;
            }),10),
            xAxis: 'id',
            yValue: 'uiAffectedDonorPercentage'
          });
          _ctrl.totalDonors = projectDonors.Total;
        });

        _ctrl.gene.projects = projects.hits;
      });

      var params = {
        filters: {gene: {id: {is: [_ctrl.gene.id] }}},
        size: 0,
        include: ['facets']
      };

      Mutations.getList(params).then(function (d) {
        _ctrl.mutationFacets = d.facets;
      });
    }


    if (_ctrl.gene.hasOwnProperty('transcripts')) {
      var geneTranscriptPromie = Genes.one(_ctrl.gene.id).handler.one('affected-transcripts').get({});

      geneTranscriptPromie.then(function(data) {
        var affectedTranscriptIds = Restangular.stripRestangular(data)[_ctrl.gene.id];

        _ctrl.gene.transcripts.forEach(function (transcript) {
          var hasProteinCoding, isAffected;

          // 1) Check if transcript has protein_coding
          hasProteinCoding = transcript.type === 'protein_coding';

          // 2) Check if transcript is affected
          isAffected = affectedTranscriptIds.indexOf(transcript.id) !== -1;

          if (hasProteinCoding && isAffected) {
            _ctrl.gene.uiProteinTranscript.push(transcript);
          }
        });
      });
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('genes') !== -1) {
        refresh();
      }
    });

    refresh();
  });

  module.controller('GeneMutationsCtrl',
    function ($scope, HighchartsService, LocationService, Genes, Projects, Donors, ProjectCache) {
      var _ctrl = this;

      function success(mutations) {
        if (mutations.hasOwnProperty('hits')) {
          var projectCachePromise = ProjectCache.getData();

          _ctrl.mutations = mutations;

          // Need to get SSM Test Donor counts from projects
          Projects.getList().then(function (projects) {
            _ctrl.mutations.hits.forEach(function (mutation) {
              mutation.advQuery = LocationService.mergeIntoFilters({mutation: {id: {is: [mutation.id]}}});

              Donors.getList({
                size: 0,
                include: 'facets',
                filters: mutation.advQuery
              }).then(function (data) {
                mutation.uiDonors = data.facets.projectId.terms;

                if (mutation.uiDonors) {
                  mutation.uiDonors.forEach(function (facet) {
                    var p = _.find(projects.hits, function (item) {
                      return item.id === facet.term;
                    });

                    facet.advQuery = LocationService.mergeIntoFilters({
                      mutation: {id: {is: [mutation.id]}},
                      donor: {projectId: {is: [facet.term]}}
                    });

                    projectCachePromise.then(function(lookup) {
                      facet.projectName = lookup[facet.term] || facet.term;
                    });

                    facet.countTotal = p.ssmTestedDonorCount;
                    facet.percentage = facet.count / p.ssmTestedDonorCount;
                  });
                }
              });
            });
          });

          _ctrl.bar = HighchartsService.bar({
            hits: _ctrl.mutations.hits,
            xAxis: 'id',
            yValue: 'affectedDonorCountFiltered'
          });
        }
      }

      function refresh() {
        Genes.one().getMutations({
          include: 'consequences',
          filters: LocationService.filters()
        }).then(success);
      }

      $scope.$on('$locationChangeSuccess', function (event, dest) {
        if (dest.indexOf('genes') !== -1) {
          refresh();
        }
      });

      refresh();
    });

  module.controller ('GeneCompoundsCtrl', function ($stateParams, CompoundsService, RouteInfoService) {
    var geneId = $stateParams.id;
    var _this = this;

    this.compoundUrl = RouteInfoService.get ('drugCompound').href;
    this.concatAtcDescriptions = function (compound) {
      var codes = _.map (_.get (compound, 'atcCodes', []), 'description');
      return _.isEmpty (codes) ? '--' : codes.join (', ');
    };

    CompoundsService.getCompoundsByGeneId (geneId).then (function (data) {
      var compounds = data.plain();

      _this.compounds = compounds;

    }, function (error) {
      console.log ('Error getting compounds related to the geneId', error);
    });
  });

})();

(function () {
  'use strict';

  var module = angular.module('icgc.genes.models', []);

  module.service('Genes', function (Restangular, LocationService, Gene) {
    this.handler = Restangular.all('genes');

    this.getList = function (params) {
      var defaults = {
        size: 10,
        from: 1,
        filters: LocationService.filters()
      };

      return this.handler.get('', angular.extend(defaults, params));
    };

    this.one = function (id) {
      return id ? Gene.init(id) : Gene;
    };
  });

  module.service('Gene', function (Restangular) {
    var _this = this;
    this.handler = {};

    this.init = function (id) {
      this.handler = Restangular.one('genes', id);
      return _this;
    };

    this.get = function (params) {
      var defaults = {};

      return this.handler.get(angular.extend(defaults, params));
    };

    this.getMutations = function (params) {
      var defaults = {};

      return this.handler.one('mutations', '').get(angular.extend(defaults, params));
    };
  });

  module.service ('GeneSymbols', function (Restangular) {
    var apiUrl = 'ui/search/gene-symbols';

    this.resolve = function (ensemblIds) {
      return Restangular.one (apiUrl, ensemblIds).get();
    };
  });

})();
