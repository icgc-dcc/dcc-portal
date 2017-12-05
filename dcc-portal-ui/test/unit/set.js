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
 * Test adding and removing sets
 */
describe('Test SetService', function() {
  var SetService, httpMock;

  beforeEach(angular.mock.module('icgc'));

  beforeEach(inject(function ($httpBackend, $q, $rootScope, _SetService_, _API_) {
    window._gaq = [];
    httpMock = $httpBackend;
    SetService = _SetService_;
    API = _API_;

    // Not sure why these are needed
    httpMock.when('GET', API.BASE_URL + '/releases/current').respond({});
    httpMock.when('GET', '/scripts/releases/views/home.html').respond({});

    httpMock.when('POST', API.BASE_URL + '/entityset?async=false').respond({
      id: 'uu-id-1',
      name: 'regular set',
      type: 'donor',
      count: 10
    });
    httpMock.when('POST', API.BASE_URL + '/entityset/union?async=false').respond({
      id: 'uu-id-2',
      name: 'derived set',
      type: 'donor',
      count: 10
    });
  }));

  it('Test adding new set', function() {
    expect(SetService.getAll().length).toEqual(0);
    var promise = SetService.addSet('donor', {filters:{}});
    httpMock.flush();

    // Flush SetService
    SetService.initService();
    expect(SetService.getAll().length).toEqual(1);
    expect(SetService.getAll()[0].name).toEqual('regular set');
  });

  it('Test adding derived set', function() {
    expect(SetService.getAll().length).toEqual(1);
    var promise = SetService.addDerivedSet('donor', {filters:{}});
    httpMock.flush();

    // Flush SetService
    SetService.initService();
    expect(SetService.getAll().length).toEqual(2);
    expect(SetService.getAll()[0].name).toEqual('derived set');
    expect(SetService.getAll()[1].name).toEqual('regular set');
  });


  it('Test removing set', function() {
    expect(SetService.getAll().length).toEqual(2);
    SetService.remove('uu-id-2');
    expect(SetService.getAll().length).toEqual(1);
    SetService.remove('uu-id-2');
    expect(SetService.getAll().length).toEqual(1);
    SetService.remove('uu-id-1');
    expect(SetService.getAll().length).toEqual(0);
  });


});
