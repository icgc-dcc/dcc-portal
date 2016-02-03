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

  var module = angular.module('icgc.genesets.services', []);

  module.service('GeneSets', function (Restangular, LocationService, GeneSet) {
    this.handler = Restangular.all('genesets');

    this.several = function(list) {
      return Restangular.several('genesets', list);
    };

    this.getList = function (params) {
      var defaults = {
        size: 10,
        from: 1,
        filters: LocationService.filters()
      };

      return this.handler.get('', angular.extend(defaults, params));
    };

    this.one = function (id) {
      return id ? GeneSet.init(id) : GeneSet;
    };
  });

  module.service('GeneSet', function (Restangular) {
    var _this = this;
    this.handler = {};

    this.init = function (id) {
      this.handler = Restangular.one('genesets', id);
      return _this;
    };

    this.get = function (params) {
      var defaults = {};

      return this.handler.get(angular.extend(defaults, params));
    };
  });

  module.service('GeneSetService', function(Donors, Mutations, Genes, Projects, Restangular) {

    /**
     * Find out which projects are affected by this gene set, this data is used to generate cancer distribution
     */
    this.getProjects = function(filters) {
      var promise = Donors.getList({
        size: 0,
        from: 1,
        include: ['facets'],
        filters: filters
      });

      return promise.then(function(data) {
        var ids = _.pluck(data.facets.projectId.terms, 'term');

        if (_.isEmpty(ids)) {
          return [];
        }

        return Projects.getList({
          filters: {'project': {'id': { 'is': ids}}}
        });
      });
    };


    ////////////////////////////////////////////////////////////////////////////////
    // Wrapper functions to make controller cleaner
    ////////////////////////////////////////////////////////////////////////////////
    this.getProjectMutations = function(ids, filters) {
      return Projects.one(ids).handler.one('mutations', 'counts').get({filters: filters});
    };

    this.getProjectDonors = function(ids, filters) {
      return Projects.one(ids).handler.one('donors', 'counts').get({filters: filters});
    };

    this.getProjectGenes = function(ids, filters) {
      return Projects.one(ids).handler.one('genes', 'counts').get({filters: filters});
    };

    this.getGeneCounts = function(f) {
      return Genes.handler.one('count').get({filters: f});
    };

    this.getMutationCounts = function(f) {
      return Mutations.handler.one('count').get({filters: f});
    };

    this.getMutationImpactFacet = function(f) {
      var params = {
        filters: f,
        size: 0,
        include: ['facets']
      };
      return Mutations.getList(params);
    };


    ////////////////////////////////////////////////////////////////////////////////
    // Reactome pathway only
    ////////////////////////////////////////////////////////////////////////////////
    this.getPathwayXML = function(pathwayId) {
      return Restangular.one('ui')
        .one('reactome').one('pathway-diagram')
        .get({'pathwayId' : pathwayId},{'Accept':'application/xml'});
    };

    this.getPathwayZoom = function(pathwayId) {
      return Restangular.one('ui')
        .one('reactome').one('pathway-sub-diagram')
        .get({'pathwayId' : pathwayId}, {'Accept':'application/json'});
    };

    this.getPathwayProteinMap = function(pathwayId, mutationImpacts) {
      return Restangular.one('ui')
        .one('reactome').one('protein-map')
        .get({
          pathwayId: pathwayId,
          impactFilter: _.isEmpty(mutationImpacts)? '': mutationImpacts.join(',')
        });
    };

  });


  /**
   * Generate hierarchical structure for gene-ontology and reactome pathways.
   * This is for displaying on the user interface
   */
  module.service('GeneSetHierarchy', function() {

    /**
     * Converts the inferred tree from a list oriented structure to a tree-like structure for UI display
     *
     * Input is a flattened list of inferred tree nodes sorted by level
     *   [
     *     { level: 0, name ... },
     *     { level: 1, name ... },
     *     { level: 1, name ... },
     *     { level: 1, name ... },
     *     { level: 2, name ... },
     *     { level: 3, name ... },
     *     { level: 3, name ... }
     *   ]
     *
     * Ouptut is a tree-like structure organized by level
     *  {
     *    level: 0
     *    goTerms: ...
     *    child: {
     *      level: 1
     *      goTerms: ...
     *      child: {
     *        level: 2
     *        goTerms: ...
     *        child: {
     *          level: 3
     *          goTerms: ...
     *        }
     *      }
     *    }
     *  }
     *
     */
    function uiInferredTree(inferredTree) {
      var root = {}, node = root, current = null;

      if (! angular.isDefined(inferredTree) || _.isEmpty(inferredTree) ) {
        return {};
      }
      current = inferredTree[0].level;

      node.goTerms = [];
      inferredTree.forEach(function(goTerm) {

        // Next level
        if ( (goTerm.level !== current || goTerm.relation === 'self') && !_.isEmpty(node.goTerms)) {
          current = goTerm.level;
          node.child = {};
          node.child.goTerms = [];
          node = node.child;
        }
        node.goTerms.push({
          name: goTerm.name,
          id: goTerm.id,
          relation: goTerm.relation,
          level: parseInt(goTerm.level, 10)
        });
      });
      return root;
    }


    /**
     * Convert reactome pathway hierarchy from a list oriented structure to a tree-like structure for UI display
     *
     * Input is a list of lists of pathways, something like
     *   [
     *     [R-HSA-1, R-HSA-2, R-HSA-3, R-HSA-self],
     *     [R-HSA-4, R-HSA-5, R-HSA-self],
     *     ...
     *   ]
     *
     * Output is a list of trees, where each tree describe a pathway hierarchy
     *   [
     *      { R-HSA-1 : { R-HSA-2: { R-HSA-3: {R-HSA-self}} } },
     *      { R-HSA-4 : { R-HSA-5: { R-HSA-self}} }
     *   ]
     */
    function uiPathwayHierarchy(hierarchy, geneSet) {
      var hierarchyList = [];
      if (! angular.isDefined(hierarchy) || _.isEmpty(hierarchy) ) {
        return hierarchyList;
      }

      hierarchy.forEach(function(path) {
        var root = {}, node = root, diagramId = '';

        // Add all ancestors
        var geneSetId = geneSet.id;

        path.forEach(function(n, idx) {
          node.id = n.id;
          node.name = n.name;

          // FIXME: just make it bool in api?
          if (n.diagrammed === 'true') {
            diagramId = node.id;
          }

          // Has children, swap
          if (idx < path.length-1) {
            node.children = [];
            node.children.push({});
            node = node.children[0];
          }
        });


        hierarchyList.push({
          'root': root,
          'diagramId': diagramId,
          'geneSetId': geneSetId
        });
      });
      return hierarchyList;
    }


    this.uiInferredTree = uiInferredTree;
    this.uiPathwayHierarchy = uiPathwayHierarchy;
  });


})();
