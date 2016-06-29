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

  var module = angular.module('icgc.mutations', ['icgc.mutations.controllers', 'icgc.mutations.services', 'ui.router']);

  module.config(function ($stateProvider) {
    $stateProvider.state('mutation', {
      url: '/mutations/:id',
      templateUrl: 'scripts/mutations/views/mutation.html',
      controller: 'MutationCtrl as MutationCtrl',
      resolve: {
        mutation: ['$stateParams', 'Mutations', function ($stateParams, Mutations) {
          return Mutations.one($stateParams.id).get({ include: ['occurrences', 'transcripts', 'consequences'] });
        }]
      }
    });
  });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.mutations.controllers', ['icgc.mutations.models']);

  module.controller('MutationCtrl', function (HighchartsService, Page, Genes, mutation) {
    var _ctrl = this, projects;
    Page.setTitle(mutation.id);
    Page.setPage('entity');

    _ctrl.gvOptions = { location: false, panels: false, zoom: 100 };

    _ctrl.mutation = mutation;
    _ctrl.mutation.uiProteinTranscript = [];

    projects = {};
    _ctrl.projects = [];

    if (_ctrl.mutation.hasOwnProperty('occurrences')) {
      _ctrl.mutation.occurrences.forEach(function (occurrence) {
        if (projects.hasOwnProperty(occurrence.projectId)) {
          projects[occurrence.projectId].affectedDonorCount++;
        } else {
          projects[occurrence.projectId] = occurrence.project;
          projects[occurrence.projectId].affectedDonorCount = 1;
        }
      });
    }

    for (var p in projects) {
      if (projects.hasOwnProperty(p)) {
        var project = projects[p];
        project.percentAffected = project.affectedDonorCount / project.ssmTestedDonorCount;
        _ctrl.projects.push(project);
      }
    }


    if (_ctrl.mutation.hasOwnProperty('consequences') && _ctrl.mutation.consequences.length) {
      var affectedGeneIds = _.filter(_.pluck(_ctrl.mutation.consequences, 'geneAffectedId'), function (d) {
        return !_.isUndefined(d);
      });

      if (affectedGeneIds.length > 0) {
        Genes.getList({
          filters: { gene: { id: { is: affectedGeneIds } } },
          field: [],
          include: 'transcripts',
          size: 100
        }).then(function (genes) {
          var geneTranscripts = _.pluck(genes.hits, 'transcripts');
          var mergedTranscripts = [];
          geneTranscripts.forEach(function (t) {
            mergedTranscripts = mergedTranscripts.concat(t);
          });

          // Using mutation transcripts to check conditions
          _ctrl.mutation.transcripts.forEach(function (transcript) {
            var hasProteinCoding, aaMutation, hasAaMutation;

            // 1) Check if transcript has protein_coding
            hasProteinCoding = transcript.type === 'protein_coding';

            // 2) Has aaMutation
            aaMutation = transcript.consequence.aaMutation;
            hasAaMutation = aaMutation && aaMutation !== '' && aaMutation !== '-999' && aaMutation !== '--';

            if (hasProteinCoding && hasAaMutation) {
              // Need to use gene transcripts here to get domains
              _ctrl.mutation.uiProteinTranscript.push(_.find(mergedTranscripts, function (t) {
                return t.id === transcript.id;
              }));
            }
          });
          _ctrl.mutation.uiProteinTranscript = _.sortBy(_ctrl.mutation.uiProteinTranscript, function (t) {
            return t.name;
          });
        });
      }
    }

    _ctrl.bar = HighchartsService.bar({
      hits: _.sortBy(_ctrl.projects, function (p) {
        return -p.percentAffected;
      }),
      xAxis: 'id',
      yValue: 'percentAffected'
    });
  });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.mutations.services', []);

  module.constant('ImpactOrder', [
    'High',
    'Medium',
    'Low',
    'Unknown',
    '_missing'
  ]);

})();

(function () {
  'use strict';

  var module = angular.module('icgc.mutations.models', []);

  module.service('Mutations', function (Restangular, FilterService, Mutation, Consequence, ImpactOrder) {
    this.handler = Restangular.all('mutations');

    this.getList = function (params) {
      var defaults = {
        size: 10,
        from: 1,
        filters: FilterService.filters()
      };

      return this.handler.get('', angular.extend(defaults, params)).then(function (data) {
        if (data.hasOwnProperty('facets')) {
          var precedence = Consequence.precedence();

          if (data.facets.hasOwnProperty('consequenceType') &&
            data.facets.consequenceType.hasOwnProperty('terms')) {
            data.facets.consequenceType.terms = data.facets.consequenceType.terms.sort(function (a, b) {
              return precedence.indexOf(a.term) - precedence.indexOf(b.term);
            });
          }
          if (data.facets.hasOwnProperty('functionalImpact') &&
            data.facets.functionalImpact.hasOwnProperty('terms')) {
            data.facets.functionalImpact.terms = data.facets.functionalImpact.terms.sort(function (a, b) {
              return ImpactOrder.indexOf(a.term) - ImpactOrder.indexOf(b.term);
            });
          }
        }

        return data;
      });
    };

    this.one = function (id) {
      return id ? Mutation.init(id) : Mutation;
    };
  });

  module.service('Mutation', function (Restangular) {
    var _this = this;
    this.handler = {};

    this.init = function (id) {
      this.handler = Restangular.one('mutations', id);
      return _this;
    };

    this.get = function (params) {
      var defaults = {};

      return this.handler.get(angular.extend(defaults, params));
    };
  });

  module.service('Occurrences', function (Restangular, FilterService, Occurrence, $q) {
    this.handler = Restangular.all('occurrences');

    this.getList = function (params) {
      var defaults = {
        size: 10,
        from: 1,
        filters: FilterService.filters()
      };

      return this.handler.get('', angular.extend(defaults, params));
    };

    this.getAll = function (params) {
      var _self = this;

      var defaults = {
        size: 10,
        from: 1,
        filters: {}
      };

      var observations = [];
      var deferred = $q.defer();

      function pageAll(params) {
        _self.handler.get('', angular.extend(defaults, params)).then(function (data) {
          observations = observations.concat(data.hits);
          var pagination = data.pagination;
          if (pagination.page < pagination.pages) {
            var newParams = params;
            newParams.from = (pagination.page + 1 - 1) * 100 + 1;
            pageAll(newParams);
          } else {
            deferred.resolve(observations);
          }
        });
      }
      
       pageAll(params);
       return deferred.promise;
    };

    this.one = function (id) {
      return id ? Occurrence.init(id) : Occurrence;
    };
  });

  module.service('Occurrence', function (Restangular) {
    var _this = this;
    this.handler = {};

    this.init = function (id) {
      this.handler = Restangular.one('occurrences', id);
      return _this;
    };

    this.get = function (params) {
      var defaults = {};

      return this.handler.get(angular.extend(defaults, params));
    };
  });

  module.service('Transcripts', function (Restangular, FilterService, Transcript) {
    this.handler = Restangular.all('occurrences');

    this.getList = function (params) {
      var defaults = {
        size: 10,
        from: 1,
        filters: FilterService.filters()
      };

      return this.handler.get('', angular.extend(defaults, params));
    };

    this.one = function (id) {
      return id ? Transcript.init(id) : Transcript;
    };
  });

  module.service('Transcript', function (Restangular) {
    var _this = this;
    this.handler = {};

    this.init = function (id) {
      this.handler = Restangular.one('transcripts', id);
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

  module.service('Protein', function (Restangular) {
    var _this = this;
    this.handler = {};

    this.init = function (id) {
      this.handler = Restangular.one('protein', id);
      return _this;
    };

    this.get = function (params) {
      var defaults = {};

      return this.handler.get(angular.extend(defaults, params));
    };
  });
})();
