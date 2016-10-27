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

describe('Test Chromosome Service', function() {
  var Chromosome;
  beforeEach(angular.mock.module('icgc'));

  beforeEach(inject(function (_Chromosome_) {
    window._gaq = [];
    Chromosome = _Chromosome_;
  }));

  it('Test chromosome length', function() {
    var t = Object.keys(Chromosome.get()).length; 
    expect(t).toEqual(25);
  });


  it('Test chromosome validation: char' , function() {
    expect(Chromosome.validate('x')).toEqual(true);
    expect(Chromosome.validate('y')).toEqual(true);
    expect(Chromosome.validate('mt')).toEqual(true);
    expect(Chromosome.validate('23')).toEqual(false);
    expect(Chromosome.validate('abc')).toEqual(false);
  });

  it('Test chromosome validation: char, start', function() {
    expect(Chromosome.validate('1', 200)).toEqual(true);
    expect(Chromosome.validate('1', 999999999)).toEqual(false);

    // Edge case
    expect(Chromosome.validate('3', 0)).toEqual(false);
    expect(Chromosome.validate('3', Chromosome.length('3')+1)).toEqual(false);
  })

  it('Test chromosome validation: char, start, end', function() {
    expect(Chromosome.validate('3', 1, 2)).toEqual(true);

    expect(Chromosome.validate('3', 9999999999, 2)).toEqual(false);
    expect(Chromosome.validate('3', 2, 9999999999)).toEqual(false);
  });



});

