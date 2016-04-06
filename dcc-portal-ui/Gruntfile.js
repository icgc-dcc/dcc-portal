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
var LIVERELOAD_PORT = 35729;
var modRewrite = require('connect-modrewrite');
var lrSnippet = require('connect-livereload')({ port: LIVERELOAD_PORT });
var mountFolder = function (connect, dir) {
  return connect.static(require('path').resolve(dir));
};

// # Globbing
// for performance reasons we're only matching one level down:
// 'test/spec/{,*/}*.js'
// use this if you want to recursively match all subfolders:
// 'test/spec/**/*.js'

module.exports = function (grunt) {
  // load all grunt tasks
  require('matchdep').filterDev('grunt-*').forEach(grunt.loadNpmTasks);
  require('time-grunt')(grunt);

  var configProvider = require('./dcc-grunt-tasks/ICGC-grunt-config-provider')(grunt);

  // configurable paths
  var yeomanConfig = {
    app: 'app',
    dist: 'target/app',
    developIndexFile: 'develop/html/index.develop.html'
  };


  try {
    yeomanConfig.app = require('./bower.json').appPath || yeomanConfig.app;
  } 
  catch (e) {
  }

  grunt.initConfig({
    'bower-install-simple': configProvider.setConfigForTask('bower-install-simple', function() {
      
        /**
        * Bower configuration
        * See: https://www.npmjs.com/package/grunt-bower-install-simple
        */
        
        var config =  {options: { color: true } };
        
        if (configProvider.isProductionBuild()) {
          config.prod = { options: { production: true, interactive: false, forceLatest: false } };
        }
        else {
          config.dev = { options: { production: false,  interactive: true, forceLatest: false } };
        }
            
        return config;  
    })
    // Gets the default dev config object in this context because
    // we have yet to set a default
    .getConfigForTask('bower-install-simple'), 
    peg: {
      pql: {
        src: './app/scripts/common/js/pql/conf/pql.pegjs',
        dest: './app/scripts/common/js/pql/pqlparser.js',
        options: {
          exportVar: 'PqlPegParser'
        }
      }
    },
    yeoman: yeomanConfig,
    watch: {
      compass: {
        files: ['<%= yeoman.app %>/styles/{,*/}*.{scss,sass}',
                '<%= yeoman.app %>/scripts/**/styles/*.{scss,sass}',
                '<%= yeoman.app %>/vendor/styles/{,*/}*.{scss,sass}'],
        tasks: ['compass:server']
      },
      injector: {
        files: ['<%= yeoman.app %>/index.html'],
        tasks: ['injector:dev']
      },
      livereload: {
        options: {
          livereload: LIVERELOAD_PORT
        },
        files: [
          '<%= yeoman.app %>/{,*/}*.html',
          '{.tmp,<%= yeoman.app %>}/styles/{,*/}*.css',
          '{.tmp,<%= yeoman.app %>}/scripts/**/styles/*.css',
          '{.tmp,<%= yeoman.app %>}/scripts/**/*.js',
          '<%= yeoman.app %>/develop/scripts/{,*/}*.js',
          '<%= yeoman.app %>/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}',
          '<%= yeoman.app %>/scripts/*/images/*.{png,jpg,jpeg,gif,webp,svg}'
        ]
      }
    },
    connect: {
      options: {
        port: 9000,
        protocol: 'http',
        // Change this to '0.0.0.0' to access the server from outside.
        hostname: 'localhost'
      },
      livereload: {
        options: {
          middleware: function (connect) {
            return [
              modRewrite([
                '!\\.html|\\images|\\.js|\\.css|\\.png|\\.jpg|\\.woff|\\.ttf|\\.svg ' + 
                '/' + yeomanConfig.developIndexFile + ' [L]'
              ]),
              lrSnippet,
              mountFolder(connect, '.tmp'),
              mountFolder(connect, yeomanConfig.app)
            ];
          }
        }
      },
      test: {
        options: {
          port: 9009,
          middleware: function (connect) {
            return [
              mountFolder(connect, '.tmp'),
              mountFolder(connect, 'test')
            ];
          }
        }
      },
      dist: {
        options: {
          middleware: function (connect) {
            return [
              modRewrite([
                '!\\.html|\\images|\\.js|\\.css|\\.png|\\.jpg|\\.woff|\\.ttf|\\.svg ' +
                '/' + yeomanConfig.developIndexFile + ' [L]'
              ]),
              mountFolder(connect, yeomanConfig.dist)
            ];
          }
        }
      }
    },
    open: {
      server: {
        url: 'http://localhost:<%= connect.options.port %>'
      }
    },
    clean: {
      options: { force: true },
      dist: {
        files: [
          {
            dot: true,
            src: [
              '<%= yeoman.dist %>/*',
              '!<%= yeoman.dist %>/.git*'
            ]
          }
        ]
      },
      cleanTempBuildFiles: {
        files: [
          {
            dot: true,
            src: [
              '.tmp'
            ]
          }
        ]
      },
      server: '.tmp'
    },
    jshint: {
      options: {
        jshintrc: '.jshintrc',
        reporter: require('jshint-stylish')
      },
      all: [
        'Gruntfile.js',
        '<%= yeoman.app %>/scripts/**/*.js',
        // Skip pqlparser.js as this is auto generated during the build
        // from the 3rd party task
        '!<%= yeoman.app %>/scripts/common/js/pql/pqlparser.js'
      ]
    },
    compass: {
      options: {
        sassDir: '<%= yeoman.app %>/styles',
        cssDir: '<%= yeoman.app %>/styles',
        generatedImagesDir: '<%= yeoman.app %>/images/generated',
        imagesDir: '<%= yeoman.app %>/images',
        javascriptsDir: '<%= yeoman.app %>/scripts',
        fontsDir: '<%= yeoman.app %>/styles/fonts',
        importPath: '<%= yeoman.app %>/vendor/styles',
        httpImagesPath: '/images',
        httpGeneratedImagesPath: '/images/generated',
        httpFontsPath: '/styles/fonts',
        relativeAssets: false,
        require: ['compass', 'bootstrap-sass', 'singularitygs', 'singularity-extras'],
        bundleExec: true,
        raw: 'Encoding.default_external = \'utf-8\'\n'
      },
      dist: {},
      server: {
        options: {
          //debugInfo: true
        }
      }
    },
    // not used since Uglify task does concat,
    // but still available if needed
    /*concat: {
     dist: {}
     },*/
    // Renames files for browser caching purposes
    filerev: {
      dist: {
        src: [
          '<%= yeoman.dist %>/scripts/{,*/**/}*.js',
          '<%= yeoman.dist %>/styles/{,*/**/}*.css',
          '<%= yeoman.dist %>/images/{,*/**/}*.{png,jpg,jpeg,gif,webp,svg}'//,
          //'<%= yeoman.dist %>/styles/fonts/*'
        ]
      }
    },
    useminPrepare: {
      html: '<%= yeoman.app %>/index.html',
      options: {
        dest: '<%= yeoman.dist %>',
        flow: {
          html: {
            steps: {
              js: ['concat'],
              css: ['concat', 'cssmin']
            },
            post: {}
          }
        }
      }
    },
    usemin: {
      html: ['<%= yeoman.dist %>/**/*.html'],
      css: [
        '<%= yeoman.dist %>/styles/{,*/}*.css'
        //'<%= yeoman.dist %>/styles/{,*/**/}*.css'
      ],
      js: [
        '<%= yeoman.dist %>/scripts/{,*/}*.js'
      ],

      options: {
        assetsDirs: ['<%= yeoman.dist %>','<%= yeoman.dist %>/images']
      }
    },
    imagemin: {
      dist: {
        files: [
          {
            expand: true,
            cwd: '<%= yeoman.app %>',
            // TODO: looks like imagemin is not processing module image deps
            // copy is doing this instead -
            // will research proper way to fix this 
            src:  [
                    '/images/{,*/}*.{png,jpg,jpeg}',
                    '/scripts/*/images/**/*.{png,jpg,jpeg}'
                  ],
            dest: '<%= yeoman.dist %>/images'
          }
        ]
      }
    },
    cssmin: {
      // By default, your `index.html` <!-- Usemin Block --> will take care of
      // minification. This option is pre-configured if you do not wish to use
      // Usemin blocks.
      // dist: {
      //   files: {
      //     '<%= yeoman.dist %>/styles/main.css': [
      //       '.tmp/styles/{,*/}*.css',
      //       '<%= yeoman.app %>/styles/{,*/}*.css'
      //     ]
      //   }
      // }
    },
    htmlmin: {
      dist: {
        options: {
          /*removeCommentsFromCDATA: true,
           // https://github.com/yeoman/grunt-usemin/issues/44
           //collapseWhitespace: true,
           collapseBooleanAttributes: true,
           removeAttributeQuotes: true,
           removeRedundantAttributes: true,
           useShortDoctype: true,
           removeEmptyAttributes: true,
           removeOptionalTags: true*/
        },
        files: [
          {
            expand: true,
            cwd: '<%= yeoman.app %>',
            src: ['*.html', 'views/**/*.html'],
            dest: '<%= yeoman.dist %>'
          }
        ]
      }
    },
    // Put files not handled in other tasks here
    copy: {
      dist: {
        files: [
          {
            expand: true,
            dot: true,
            cwd: '<%= yeoman.app %>',
            dest: '<%= yeoman.dist %>',
            src: [
              '*.{ico,png,txt}',
              '.htaccess',
              'bower_components/**',

              // 'vendor/scripts/angularjs/*',
              'vendor/styles/genomeviewer/**/*',
              'styles/images/**/*.{gif,webp,svg,png,jpg}',
              'scripts/*/images/**/*.{gif,webp,svg,png,jpg}',
              'styles/fonts/*',
              'views/**/*',
              'scripts/**/*.html',
              'scripts/**/*.map',
              'data/*'
            ]
          },
          {
            expand: true,
            cwd: '.tmp/images',
            dest: '<%= yeoman.dist %>/images',
            src: [
              'generated/*'
            ]
          },
          // Genome viewer
          {
            expand: true,
            flatten: true,
            dot: true,
            cwd: '<%= yeoman.app %>',
            dest: '<%= yeoman.dist %>/styles/fonts/',
            src: ['vendor/scripts/genome-viewer/vendor/fontawesome/fonts/*' ]
          }
        ]
      }
    },
    concurrent: {
      server: [
        'compass:server'
      ],
      test: [
        'compass'
      ],
      dist: [
        //'compass:dist',
        'imagemin',
        'htmlmin'
      ]
    },
    karma: {
      unit: {
        configFile: './karma.conf.js',
        singleRun: true
      }
    },
    cdnify: {
      dist: {
        html: ['<%= yeoman.dist %>/*.html']
      }
    },
    // ngAnnotate tries to make the code safe for minification automatically by
    // using the Angular long form for dependency injection. It doesn't work on
    // things like resolve or inject so those have to be done manually.
    ngAnnotate: {
      dist: {
        files: [{
          expand: true,
          cwd: '<%= yeoman.dist %>/scripts',
          src: '*.js',
          dest: '<%= yeoman.dist %>/scripts'
        }]
      }
    },
    uglify: {
      generated: {
        options: {
          sourceMap: true,
          compress: true,
          mangle: true,
          sourceMapIncludeSources: true
        },
        files: [{
          expand: true,
          cwd: '<%= yeoman.dist %>/scripts',
          src: [
            '*.js'
          ],
          dest: '<%= yeoman.dist %>/scripts',
          ext: '.js'
        }]

      }
    },
    jsdoc2md: {
      oneOutputFile: {
        src: '<%= yeoman.dist %>/scripts/scripts.js',
        dest: '<%= yeoman.dist %>/docs/api/README.md'
      }
    },
    injector: {
      options: {},
      dev: {
        options: {
          template: '<%= yeoman.app %>/index.html',
          destFile: '<%= yeoman.app %>/<%= yeoman.developIndexFile %>',
          relative: false,
          ignorePath: 'app'
        },
        files: [{
          expand: true,
          cwd: '<%= yeoman.app %>/develop/scripts',
          src: ['*.js']
        }]
      }
    }
  });

  grunt.registerTask('bower-install', ['bower-install-simple']);
  
  grunt.registerTask('server', function (target) {
    if (target === 'dist') {
      return grunt.task.run(['build',
        'connect:dist:keepalive']);
    }

    grunt.task.run([
      'ICGC-setBuildEnv:development',
      'injector:dev',
      'clean:server',
      'concurrent:server',
      'connect:livereload',
      //'open',
      'watch'
    ]);
  });

  grunt.registerTask('test', [
    'clean:server',
    'concurrent:test',
    'connect:test',
    'karma'
  ]);

  grunt.registerTask('build', [
    'ICGC-setBuildEnv:production',
    'clean:dist',
    'compass:dist', // run in case files were changed outside of grunt server (dev environment)
    'bower-install',
    'jshint',
    'peg',
    'karma',
    'useminPrepare',
    'concurrent:dist',
    'concat',
    'copy:dist',
    //'jsdoc2md',
//    'cdnify',
    'ngAnnotate',
    'cssmin',
    'uglify',
    'filerev',
    'usemin',
    'clean:cleanTempBuildFiles'
  ]);

  grunt.registerTask('default', [
    //'jshint',
    'test',
    'build'
  ]);
};
