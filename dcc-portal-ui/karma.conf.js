module.exports = function(config){
    config.set({
    //  root path location that will be used to resolve all relative paths in files and exclude sections, should be the root of your project
    // basePath : '../',
    basePath : '.',

    // files to include, ordered by dependencies
    files : [
      // include relevant Angular files and libs
      'app/bower_components/jquery/dist/jquery.js',
      'app/bower_components/lodash/lodash.js',
      'app/bower_components/angularjs/angular.js',
      'app/bower_components/angular-sanitize/angular-sanitize.js',
      'app/bower_components/angular-animate/angular-animate.js',
      'app/bower_components/angular-cookies/angular-cookies.js',
      'app/bower_components/angular-loading-bar/build/loading-bar.js',
      'app/bower_components/restangular/dist/restangular.js',
      'app/bower_components/angular-ui-router/release/angular-ui-router.js',
      'app/bower_components/ngInfiniteScroll/build/ng-infinite-scroll.js',
      'app/bower_components/angular-local-storage/dist/angular-local-storage.js',
      'app/bower_components/angularytics/dist/angularytics.js',
      'app/bower_components/angular-ui-utils/scrollfix.js',
      'app/bower_components/angular-markdown-directive/markdown.js',
      'app/bower_components/angular-lodash/angular-lodash.js',
      'app/bower_components/angular-bootstrap/ui-bootstrap-tpls.min.js',
      'app/bower_components/angularjs-toaster/toaster.js',
      'app/bower_components/zeroclipboard/dist/ZeroClipboard.js',
      'app/bower_components/highcharts/highcharts.js',
      'app/bower_components/highcharts/modules/no-data-to-display.src.js',
      'app/bower_components/d3/d3.min.js',
      'app/bower_components/Blob/Blob.js',
      'app/bower_components/FileSaver/FileSaver.min.js',
      'app/bower_components/showdown/src/showdown.js',
      'app/bower_components/showdown/src/extensions/table.js',
      'app/bower_components/x2js/xml2json.js',
      'app/vendor/scripts/table2CSV.js',
      'app/vendor/scripts/oncogrid/oncogrid-debug.js',

      // === Test, mock files ===
      'app/bower_components/angular-mocks/angular-mocks.js',

      // === Application files ===
      'app/scripts/app/js/app.js',
      'app/scripts/**/*.js',

      // include unit test specs
      'test/unit/**/*.js'
    ],

    // files to exclude
    exclude : [
        'app/lib/angular/angular-loader.js'
      , 'app/lib/angular/*.min.js'
      , 'app/lib/angular/angular-scenario.js'
    ],

    // karma has its own autoWatch feature but Grunt watch can also do this
    autoWatch : false,

    // testing framework, be sure to install the karma plugin
    frameworks: ['jasmine'],

    // browsers to test against, be sure to install the correct karma browser launcher plugin
    // browsers : ['Chrome', 'PhantomJS', 'Firefox'],
    // browsers : ['Chrome'],
    browsers : ['PhantomJS'],

    // progress is the default reporter
    // reporters: ['progress'],
    reporters: ['spec'],

    // map of preprocessors that is used mostly for plugins
    preprocessors: {

    },

    // list of karma plugins
    plugins : [
        'karma-spec-reporter',
        'karma-chrome-launcher',
        'karma-jasmine',
        'karma-phantomjs-launcher'
    ]
})}
