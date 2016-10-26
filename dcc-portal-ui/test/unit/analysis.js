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

/**
 * Test adding and removing analysis
 */
describe('Test AnalysisService', function() {

  var AnalysisService, httpMock;
  beforeEach(angular.mock.module('icgc'));

  beforeEach(inject(function ($httpBackend, $q, $rootScope, _AnalysisService_) {
    window._gaq = [];
    httpMock = $httpBackend;
    AnalysisService = _AnalysisService_;

    // Not sure why these are needed
    httpMock.when('GET', '/api/v1/releases/current').respond({});
    httpMock.when('GET', 'views/home.html').respond({});
  }));


  it('Adding analyses', function() {
    AnalysisService.addAnalysis({
      id: 'analysis-1',
      inputCount: 3,
      type: 'phenotype'
    }, 'phenotype');

    AnalysisService.addAnalysis({
      id: 'analysis-2',
      inputCount: 3,
      type: 'union',
    }, 'union');

    AnalysisService.addAnalysis({
      id: 'analysis-3',
      params: {
        universe: 'REACTOME', 
        maxGeneCount: 100,
      },
      type: 'enrichment'
    }, 'enrichment');

    expect(AnalysisService.getAll().length).toEqual(3);
  });

  it('Adding duplicated analysis', function() {
    AnalysisService.addAnalysis({
      id: 'analysis-1',
      inputCount: 3,
      type: 'phenotype'
    }, 'phenotype');

    expect(AnalysisService.getAll().length).toEqual(3);
  });

  it('Removing analyses', function() {
    AnalysisService.remove('analysis-3');

    expect(AnalysisService.getAll().length).toEqual(2);
    var t = _.filter(AnalysisService.getAll(), function(analysis) {
      return analysis.id === 'analysis-3';
    }).length;
    expect(t).toEqual(0);
  });

  it('Removing all', function() {
    AnalysisService.removeAll();
    expect(AnalysisService.getAll().length).toEqual(0);
  });


});
