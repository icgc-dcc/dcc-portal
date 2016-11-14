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

'use strict';

angular.module('highcharts', ['highcharts.directives', 'highcharts.services']);

angular.module('highcharts.directives', [])
  .constant('highchartsConstants', {
    DEFAULT_NO_DATA_SETTINGS: {
      lang: {
        noData: 'No data to display.'
      },
      noData: {
        style: {
          fontWeight: '400',
          fontSize: '1rem',
          color: '#777'
        }
      }
    }
  })
  .service('highchartsService', function(highchartsConstants) {
    var _service = this;

    _service.getCustomNoDataConfig = function(shouldShowNoDataMsg) {
      var defaultNoDataConfig = highchartsConstants.DEFAULT_NO_DATA_SETTINGS,
          shouldShowNoDataMessage = typeof shouldShowNoDataMsg === 'string' &&
                                    shouldShowNoDataMsg.toLowerCase() === 'false'  ? false : true;

      if (! shouldShowNoDataMessage) {
        defaultNoDataConfig.noData.style.display = 'none';
      }

      return defaultNoDataConfig;
    };

  });

angular.module('highcharts.directives').directive('pie', function (Facets, $filter, ValueTranslator,
                                                                   highchartsService) {
  function ensureArray (array) {
    return _.isArray (array) ? array : [];
  }
  var isEmptyArray = _.flow (ensureArray, _.isEmpty);

  return {
    restrict: 'E',
    replace: true,
    scope: {
      items: '=',
      groupPercent: '@',
      shouldShowNoDataMessage: '@',
      configOverrides: '&'
    },
    template: '<span style="margin: 0 auto">not working</span>',
    link: function ($scope, $element, $attrs) {
      // Defaults to 5%
      $scope.groupPercent = $scope.groupPercent || 5;

      var enrichDatum = function (datum) {
        datum.term = datum.name;

        return datum;
      };

      var summarize = function (items) {
        var count = _.size (items);

        if (count < 2) {
          return items.map (enrichDatum);
        }

        var firstItem = _.first (items);

        return {
          name: 'Others (' + count + ' ' + $attrs.heading + 's)',
          color: '#999',
          y: _.sum (items, 'y'),
          type: firstItem.type,
          facet: firstItem.facet,
          term: _.map (items, 'name')
        };
      };

      var transformSeriesData = function (data) {
        if (isEmptyArray (data)) {
          return [];
        }

        // Separates data into two groups, one with a defined 'name' attribute & one without.
        var separated = _.partition (data, 'name');
        var withName = _.first (separated);
        var withoutName = _.last (separated).map (function (datum) {
          datum.color = '#E0E0E0';
          datum.term = null;

          return datum;
        });

        var max = _.max (withName, function (datum) {
          return datum.y;
        });

        var isBelowGroupPercent = function (datum) {
          return (datum.y / max.y) < ($scope.groupPercent / 100);
        };

        // Further seperation per the rule of isBelowGroupPercent()
        separated = _.partition (withName, isBelowGroupPercent);
        var belowGroupPercent = _.first (separated);
        var regular = _.last (separated);

        // Combines all the groups into one collection.
        var result = regular.map (enrichDatum)
          .concat (withoutName)
          .concat (summarize (belowGroupPercent));

        return result;
      };

      var chartsDefaults = {
        credits: {enabled: false},
        chart: {
          renderTo: $element[0],
          type: 'pie',
          spacingTop: 2,
          spacingBottom: 2,
          marginTop: 12,
          height: $attrs.height || null,
          width: $attrs.width || null
        },
        title: {
          text: $attrs.heading,
          margin: 5,
          style: {
            fontSize: '1.25rem'
          }
        },
        plotOptions: {
          pie: {
            borderWidth: 1,
            animation: false,
            cursor: 'pointer',
            showInLegend: false,
            events: {
              click: function (e) {
                if (angular.isArray(e.point.term)) {
                  Facets.setTerms({
                    type: e.point.type,
                    facet: e.point.facet,
                    terms: e.point.term
                  });
                } else {
                  Facets.toggleTerm({
                    type: e.point.type,
                    facet: e.point.facet,
                    term: e.point.name
                  });
                }
                $scope.$apply();
              }
            },
            dataLabels: {
              enabled: false,
              distance: 10,
              connectorPadding: 7,
              formatter: function () {
                if (this.point.percentage > 5) {
                  if (this.point.y > 999) {
                    var v = this.point.y.toString();
                    v = v.substring(0, v.length - 3);
                    return $filter('number')(v) + 'k';
                  } else {
                    return $filter('number')(this.point.y);
                  }
                }
              }
            }
          },
          series: {
              point: {
                events: {
                  mouseOver: function (event) {
                    var name = event.target.term ?
                                  ValueTranslator.translate(event.target.name, event.target.facet) : 'No Data';
                    $scope.$emit('tooltip::show', {
                      element: angular.element(this),
                      text: '<div>' +
                     '<strong>' + name + '</strong><br/>' +
                     Highcharts.numberFormat(event.target.y, 0) + ' ' + event.target.series.name +
                     '</div>',
                      placement: 'top',
                      sticky: true
                    });
                  },
                  mouseOut: function () {
                    $scope.$emit('tooltip::hide');
                  }
                }
              }
            }
          },
          tooltip: {
            enabled: false
          },
          series: [
            {
              type: 'pie',
              size: '90%',
              name: $attrs.label,
              data: transformSeriesData ($scope.items)
            }
          ]
        };

      jQuery.extend(
        true, 
        chartsDefaults, 
        highchartsService.getCustomNoDataConfig($scope.shouldShowNoDataMessage),
        $scope.configOverrides());

      $scope.$watch('items', function (newValue) {
        if (!newValue) {
          return;
        }
        c.series[0].setData (transformSeriesData (newValue), true);
      });
      var c = new Highcharts.Chart (chartsDefaults);

      $scope.$on('$destroy', function () {
        c.destroy();
      });
    }
  };
});

angular.module('highcharts.directives').directive('donut', function ($rootScope, ProjectCache, $state, Facets,
                                                                     highchartsService, gettextCatalog) {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      items: '=',
      subTitle: '@',
      shouldShowNoDataMessage: '@'
    },
    template: '<div id="container" style="margin: 0 auto">' + gettextCatalog.getString('not working') + '</div>',
    link: function ($scope, $element, $attrs) {
      var c, renderChart, chartsDefaults;
      var projectLookup = {};

      renderChart = function (settings) {
        if (c) {
          c.destroy();
        }
        c = new Highcharts.Chart(settings);
      };

      chartsDefaults = {
        credits: {enabled: false},
        chart: {
          renderTo: $element[0],
          type: 'pie',
          height: $attrs.height|| null,
          width: $attrs.width || null,
          marginBottom: 60
        },
        title: {
          text: $attrs.heading,
          margin: 25,
          style: {
            fontSize: '1.25rem'
          }
        },
        subtitle: {
          text: '',
          style: {
            color: 'hsl(0, 0%, 60%)',
            paddingBottom: '25px'
          }
        },
        plotOptions: {
          pie: {
            allowPointSelect: false,
            borderWidth: 1,
            animation: true,
            cursor: 'pointer',
            showInLegend: false,
            events: {
              click: function (e) {
                if ($attrs.home) {
                  var type = e.point.type;
                  var facet = e.point.facet;
                  var name = e.point.name;

                  var filters = {};
                  filters[type] = {};
                  filters[type][facet] = {};
                  filters[type][facet].is = [name];

                  $state.go('projects', {filters: angular.toJson(filters)});
                } else {
                  Facets.toggleTerm({
                    type: e.point.type,
                    facet: e.point.facet,
                    term: e.point.name
                  });
                }
                $scope.$apply();
              }
            }
          },
          series: {
            point: {
              events: {
                mouseOver: function (event) {
                  var name = projectLookup[event.target.name] || event.target.name;

                  $scope.$emit('tooltip::show', {
                    element: angular.element(this),
                    text: '<div>' +
                   '<strong>' + name + '</strong><br>' +
                   Highcharts.numberFormat(event.target.y, 0) + ' ' + event.target.series.name +
                   '</div>',
                    placement: 'right',
                    sticky: true
                  });
                },
                mouseOut: function () {
                  $scope.$emit('tooltip::hide');
                }
              }
            }
          }
        },
        tooltip: {
          enabled: false
        },
        series: [
          {
            name: $attrs.innerLabel,
            size: '99%',
            dataLabels: {
              enabled: false,
              color: '#fff',
              connectorColor: '#000000',
              zIndex: 0,
              formatter: function () {
                if (this.point.percentage > 5) {
                  return this.point.y;
                }
              }
            }
          },
          {
            name: $attrs.outerLabel,
            size: '120%',
            innerSize: '70%',
            dataLabels: {
              enabled: false,
              overflow: 'justify',
              formatter: function () {
                if (this.point.percentage > 3) {
                  return this.point.y;
                }
              }
            }
          }
        ]
      };

      jQuery.extend(true, chartsDefaults, highchartsService.getCustomNoDataConfig($scope.shouldShowNoDataMessage));

      $scope.$watch('items', function (newValue) {
        var deepCopy, newSettings, promise;
        if (!newValue) {
          return;
        }

        promise = ProjectCache.getData();

        // We need deep copy in order to NOT override original chart object.
        // This allows us to override chart data member and still the keep
        // our original renderTo will be the same
        deepCopy = true;
        newSettings = {};
        jQuery.extend(deepCopy, newSettings, chartsDefaults);
        newSettings.series[0].data = newValue.inner;
        newSettings.series[1].data = newValue.outer;

        newSettings.subtitle.text = $scope.subTitle;
        promise.then(function(data) {
          projectLookup = data;
          renderChart(newSettings);
        });
      });

      renderChart(chartsDefaults);

      $scope.$on('$destroy', function () {
        c.destroy();
      });
    }
  };
});

angular.module('highcharts.directives')
  .directive('groupedBar', function ($location, highchartsService) {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      items: '=',
      colours: '=',
      shouldShowNoDataMessage: '@'
    },
    template: '<div id="container" style="margin: 0 auto">not working</div>',
    link: function ($scope, $element, $attrs) {
      var c, renderChart, chartsDefaults;
      renderChart = function (settings) {
        if (c) {
          c.destroy();
        }
        c = new Highcharts.Chart(settings);
      };

      chartsDefaults = {
        credits: {enabled: false},
        chart: {
          renderTo: $element[0],
          type: 'column',
          height: $attrs.height || null,
          width: $attrs.width || null
        },
        /* D3 cat 10 */
        colors: $scope.colours || ['#1f77b4', '#ff7f0e', '#2ca02c'],
        title: {
          text: $attrs.heading || '',
          margin: 25,
          style: {
            fontSize: '1.25rem'
          }
        },
        xAxis: {
          labels: {
            rotation: -45,
            align: 'right',
            x: 10,
            formatter: function () {
              if (this.value.length > 15) {
                return this.value.substring(0, 15) + '...';
              } else {
                return this.value;
              }
            }
          },
          categories: angular.isDefined($scope.items) ? $scope.items.categories : []
        },
        tooltip: {
          enabled: false
        },
        legend: {
          enabled: false
        },
        yAxis: {
          min: 0,
          showFirstLabel: true,
          showLastLabel: true,
          title: {
            text: $attrs.ylabel,
            style: {
              color: 'hsl(0, 0%, 60%)',
              fontSize: '0.75rem',
              fontWeight: '300'
            },
            margin: 5
          },
          labels: {
            enabled: true,
            formatter: function () {
              if ($attrs.format === 'percentage') {
                return this.value * 100;
              }
              return this.value;
            }
          }
        },
        series: angular.isDefined($scope.items) ? $scope.items.series : [],
        plotOptions: {
          column: {
            pointPadding: 0.10,
            borderWidth: 0,
            events: {
              click: function (e) {
                if (e.point.link) {
                  $location.path(e.point.link);
                  $scope.$apply();
                }
              }
            }
          },
          series: {
            stickyTracking : true,
            point: {
              events: {
                mouseOver: function (event) {
                  var getLabel = function () {
                    var num;
                    if ($attrs.format && $attrs.format === 'percentage') {
                      num = Number(event.target.y * 100).toFixed(2);
                    } else {
                      num = event.target.y;
                    }
                    return '<div>' +
                           // '<strong>' + event.target.category + ' - ' + event.target.series.name + '</strong><br>' +
                           '<strong>' + event.target.series.name + '</strong><br>' +
                           num +  $attrs.ylabel + ' (' + event.target.count + ')' +
                           '</div>';
                  };
                  $scope.$emit('tooltip::show', {
                    element: angular.element(this),
                    placement:'right',
                    text: getLabel(),
                    sticky:true
                  });
                },
                mouseOut: function () {
                  $scope.$emit('tooltip::hide');
                }
              }
            }
          }
        }
      };

      jQuery.extend(true, chartsDefaults, highchartsService.getCustomNoDataConfig($scope.shouldShowNoDataMessage));

      $scope.$watch('items', function (newValue) {
        var deepCopy, newSettings;

        if (!newValue) {
          return;
        }
        // We need deep copy in order to NOT override original chart object.
        // This allows us to override chart data member and still the keep
        // our original renderTo will be the same
        deepCopy = true;
        newSettings = {};
        jQuery.extend(deepCopy, newSettings, chartsDefaults);
        newSettings.xAxis.categories = newValue.x;

        if (!$attrs.format || $attrs.format !== 'percentage') {
          if (newSettings.yAxis) {
            newSettings.yAxis.allowDecimals = false;
          }
        }

        newSettings.series = newValue.series;
        newSettings.xAxis.categories = newValue.categories;
        renderChart(newSettings);
      }, true);

      renderChart(chartsDefaults);

      $scope.$on('$destroy', function () {
        c.destroy();
      });
    }
  };
});



angular.module('highcharts.directives').directive('bar', function ($location, highchartsService) {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      items: '=',
      shouldShowNoDataMessage: '@',
      configOverrides: '&',
      onRender: '&'
    },
    template: '<div id="container" style="margin: 0 auto">not working</div>',
    link: function ($scope, $element, $attrs) {
      var c, renderChart, chartsDefaults;

      var onRender = $scope.onRender();

      renderChart = function (settings) {
        if (c) {
          c.destroy();
        }
        c = new Highcharts.Chart(settings, onRender);
      };

      chartsDefaults = {
        credits: {enabled: false},
        chart: {
          renderTo: $element[0],
          type: 'column',
          height: $attrs.height || null,
          width: $attrs.width || null
        },
        title: {
          text: $attrs.heading || '',
          margin: 25,
          style: {
            fontSize: '1.25rem'
          }
        },
        subtitle: {
          text: $attrs.subheading || '',
          style: {
            color: 'hsl(0, 0%, 60%)'
          }
        },
        xAxis: {
          labels: {
            rotation: -45,
            align: 'right',
            x: 10,
            formatter: function () {
              if (this.value.length > 15) {
                return this.value.substring(0, 15) + '...';
              } else {
                return this.value;
              }
            }
          },
          categories: angular.isDefined($scope.items) ? $scope.items.x : []
        },
        tooltip: {
          enabled: false,
        },
        yAxis: {
          min: 0,
          showFirstLabel: true,
          showLastLabel: true,
          title: {
            text: $attrs.ylabel,
            style: {
              color: 'hsl(0, 0%, 60%)',
              fontSize: '0.75rem',
              fontWeight: '300'
            },
            margin: 15
          },
          labels: {
            enabled: true,
            formatter: function () {
              if ($attrs.format === 'percentage') {
                return this.value * 100;
              }
              return this.value;
            }
          }
        },
        series: angular.isDefined($scope.items) ? [
          {data: $scope.items.s}
        ] : [
          {data: []}
        ],
        plotOptions: {
          column: {
            events: {
              click: function (e) {
                if (e.point.link) {
                  $location.path(e.point.link);
                  $scope.$apply();
                }
              }
            }
          },
          series: {
            stickyTracking : true,
            point: {
              events: {
                mouseOver: function (event) {
                  var getLabel = function () {
                    var num;
                    if ($attrs.format && $attrs.format === 'percentage') {
                      num = Number(event.target.y * 100).toFixed(2);
                    } else {
                      num = Highcharts.numberFormat(event.target.y, 0);
                    }

                    return '<div>' +
                           '<strong>' + event.target.category + '</strong><br/>' +
                           num + ' ' + $attrs.ylabel +
                           '</div>';
                  };
                  $scope.$emit('tooltip::show', {
                    element: angular.element(this),
                    placement:'right',
                    text: getLabel(),
                    sticky:true
                  });
                },
                mouseOut: function () {
                  $scope.$emit('tooltip::hide');
                }
              }
            }
          }
        }
      };

      jQuery.extend(
        true,
        chartsDefaults,
        highchartsService.getCustomNoDataConfig($scope.shouldShowNoDataMessage),
        $scope.configOverrides()
      );

      $scope.$watch('items', function (newValue) {
        var deepCopy, newSettings;

        if (!newValue) {
          return;
        }
        // We need deep copy in order to NOT override original chart object.
        // This allows us to override chart data member and still the keep
        // our original renderTo will be the same
        deepCopy = true;
        newSettings = {};
        jQuery.extend(deepCopy, newSettings, chartsDefaults);
        newSettings.xAxis.categories = newValue.x;

        if (!$attrs.format || $attrs.format !== 'percentage') {
          if (newSettings.yAxis) {
            newSettings.yAxis.allowDecimals = false;
          }
        }

        newSettings.series = [
          {data: newValue.s}
        ];
        renderChart(newSettings);
      }, true);

      renderChart(chartsDefaults);

      $scope.$on('$destroy', function () {
        c.destroy();
      });
    }
  };
});
