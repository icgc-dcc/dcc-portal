/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Based on work from iobio: https://github.com/iobio
 *
 * This file incorporates work covered by the following copyright and permission notice:
 *
 *    The MIT License (MIT)
 *
 *    Copyright (c) <2014>
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 *    associated documentation files (the "Software"), to deal in the Software without restriction,
 *    including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *    and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *    subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included
 *    in all copies or substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *    THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *    TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *    SOFTWARE.
 */

function donutD3() {
  var radius = 90;

  var arc = d3.svg.arc()
    .outerRadius(radius - 10)
    .innerRadius(radius - 17);

  var formatter = d3.format(",.1f");
  var commaFormatter = d3.format(",0f");

  function my(selection) {
    // c = c || "rgb(45,143,193)";
    // var calpha = c.replace(")", ",0.2)").replace("rgb", "rgba");
    var g = selection.enter().append("g")
      .attr("class", "arc")
      .attr("transform", "translate(115,80)");

    if (g.data()[0] != undefined)
      var total = g.data()[0].data + g.data()[1].data
    else
      var total = selection.data()[0].data + selection.data()[1].data

    g.append("path")
      .attr("d", arc)
      .attr("class", function (d, i) {
        if (i == 1) return "alpha";
        return "fill"
      });

    selection.exit().remove();
    g.append("text")
      .attr("dy", "0.7em")
      .style("text-anchor", "middle")
      .attr("class", "percent")
      .attr("transform", "translate(0, -20)")
      .text(function (d, i) {
        if (i == 0) return formatter(d.data / total * 100) + "%";
      });
    g.append("text")
      .attr("dy", "1.9em")
      .style("text-anchor", "middle")
      .attr("class", "total")
      .attr("transform", "translate(0, -20)")
      .text(function (d, i) {
        if (i == 0) return commaFormatter(d.data);
      });

    selection.select("path")
      .attr("d", arc)

    selection.select(".percent")
      .text(function (d, i) {
        if (i == 0) return formatter(d.data / total * 100) + "%";
      });

    selection.select(".total")
      .text(function (d, i) {
        if (i == 0) return commaFormatter(d.data);
      });


  }

  my.radius = function (value) {
    if (!arguments.length) return radius;
    radius = value;
    arc = d3.svg.arc()
      .outerRadius(radius)
      .innerRadius(radius - 17);
    return my;
  };

  my.klass = function (value) {
    if (!arguments.length) return klass;
    klass = value;
    return my;
  };

  return my;
}