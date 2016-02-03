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

/*
describe('Test LocationService', function() {
  var LocationService;
  beforeEach(module('icgc'));

  beforeEach(inject(function (_LocationService_) {
    LocationService = _LocationService_;
  }));

  it('Test set filters', function() {
    LocationService.setFilters({donor:{id:{is:['DO00000']}}});
    var f = LocationService.mergeIntoFilters({gene:{id:{is:['ENSG00000']}}});

    expect(f).toEqual({
      donor: {id:{is:['DO00000']}},
      gene: {id:{is:['ENSG00000']}},
    });

    LocationService.clear();
    f = LocationService.filters();
    expect(f).toEqual({});
  });
});


describe('Test DefinitionService', function() {
  beforeEach(module('icgc'));
  var httpBackend, DefinitionService;

  beforeEach(inject(function (_DefinitionService_, $httpBackend) {
    DefinitionService = _DefinitionService_;
    httpBackend = $httpBackend;
  }));

  it('Check definition of SSM', function() {
    var ssm = DefinitionService.getDefinitions()['SSM'];
    expect(ssm).toEqual('Simple Somatic Mutation');
  });

});
*/
