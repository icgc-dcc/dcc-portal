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

angular.module('icgc.ui', [
  'icgc.ui.suggest',
  'icgc.ui.table',
  'icgc.ui.toolbar',
  'icgc.ui.openin',
  'icgc.ui.trees',
  'icgc.ui.lists',
  'icgc.ui.query',
  'icgc.ui.events',
  'icgc.ui.validators',
  'icgc.ui.tooltip',
  'icgc.ui.scroll',
  'icgc.ui.fileUpload',
  'icgc.ui.badges',
  'icgc.ui.copyPaste',
  'icgc.ui.popover',
  'icgc.ui.numberTween',
  'icgc.ui.iobio',
  'icgc.ui.loader',
  'icgc.ui.splitButtons'
]);


angular.module('app.ui', [
  'app.ui.tpls',
  'app.ui.toggle', 'app.ui.nested', 'app.ui.mutation',
  'app.ui.hidetext', 'app.ui.exists'
]);

/**
 * File chooser detector
 *
 * See: https://github.com/angular/angular.js/issues/1375
 * See: http://uncorkedstudios.com/blog/multipartformdata-file-upload-with-angularjs
 */
angular.module('icgc.ui.fileUpload', []).directive('fileUpload', function($parse) {
  return {
    restrict: 'A',
    link: function($scope, $element, $attrs) {
      var model = $parse($attrs.fileUpload);
      var modelSetter = model.assign;

      $element.bind('change', function() {
        $scope.$apply(function() {
          modelSetter($scope, $element[0].files[0]);

          // Trick the input so the same file will trigger another 'change' event
          // Used for gene list upload to textarea
          if ($element[0].value) {
            $element[0].value = '';
          }

        });
      });
    }
  };
});


/**
 * Renders a check or "--", this is bit faster than directly manipulationg
 * the view templates with ng-if's
 */
angular.module('app.ui.exists', []).directive('exists', function () {
  return {
    restrict: 'A',
    replace: true,
    scope: {
      exists: '='
    },
    template: '<span></span>',
    link: function(scope, element) {
      var iconOK = angular.element('<i>').addClass('icon-ok');

      function update() {
        element.empty();
        if (scope.exists) {
          element.append(iconOK);
        } else {
          element.append('--');
        }
      }
      update();

      scope.$watch('exists', function(n, o) {
        if (n === o) {
          return;
        }
        update();
      });

      scope.$on('$destroy', function() {
        iconOK.remove();
        iconOK = null;
      });
    }
  };
});

angular.module('app.ui.hidetext', []).directive('hideText', function () {
  return {
    restrict: 'E',
    replace: true,
    transclude: true,
    scope: {
      class: '@',
      highlightFilter: '='
    },
    template: '<div class="t_sh {{class}}">' +
              '<span data-ng-bind-html="text | highlight: highlightFilter"></span>' +
              '<div ng-if="text.length>=limit" class="t_sh__toggle">' +
              '<a ng-click="toggle()" href="" class="t_tools__tool">' +
              '<span ng-if="!expanded"><i class="icon-caret-down"></i> more</span>' +
              '<span ng-if="expanded"><i class="icon-caret-up"></i> less</span>' +
              '</a>' +
              '</div>' +
              '</div>',
    link: function (scope, element, attrs) {
      var previous, next;

      scope.limit = attrs.textLimit || 250;

      previous = attrs.text;
      next = attrs.text.length > scope.limit ? attrs.text.slice(0, scope.limit) + '...' : attrs.text;
      scope.text = next;

      scope.toggle = function () {
        previous = [next, next = previous][0];
        scope.text = next;
        scope.expanded = !scope.expanded;
      };
    }
  };
});

// This might be more confusing than helpful - DC
angular.module('app.ui.nested', []).directive('nested', function ($location) {
  return function (scope, element, attrs) {
    element.bind('click', function (e) {
      e.preventDefault();
      scope.$apply(function () {
        $location.path(attrs.href);
      });
    });
  };
});


/**
 * Breaks down consequences, group by consequence type, then gene, then aa mutations
 */
angular.module('app.ui.mutation', []).directive('mutationConsequences', function ($filter, Consequence) {
  return {
    restrict: 'E',
    scope: {
      items: '='
    },
    template: '<ul class="unstyled">' +
              '<li data-ng-repeat="c in consequences">' +
                '<abbr data-tooltip="SO term: {{ c.consequence }}">{{ translate(c.consequence) }}</abbr>' +
                '<span data-ng-repeat="(gk, gv) in c.data">' +
                  '<span>{{ $first==true? ": " : ""}}</span>' +
                  '<a href="/genes/{{gk}}"><em>{{gv.symbol}}</em></a> ' +
                  '<span data-ng-repeat="aa in gv.aaChangeList">' +
                    '<span class="t_impact_{{aa.FI | lowercase }}">{{aa.aaMutation}}</span>' +
                    '<span>{{ $last === false? ", " : ""}}</span>' +
                  '</span>' +
                  '<span>{{ $last === false? " - " : "" }}</span>' +
                '</span>' +
                '<span class="hidden">{{ $last === false? "|" : "" }}</span>' + // Separator for html download
              '</li>' +
              '</ul>',
    link: function (scope) {
      var consequenceMap;

      scope.translate = function(consequence) {
        return Consequence.translate( consequence );
      };

      // Massage the tabular data into the following format:
      // Consequence1: gene1 [aa1 aa2] - gene2 [aa1 aa2] - gene3 [aa3 aa4]
      // Consequence2: gene1 [aa1 aa2] - gene5 [aa1 aa2]
      consequenceMap = {};
      scope.items.forEach(function (consequence) {
        var geneId, type;

        geneId = consequence.geneAffectedId;
        type = consequence.type;

        if (geneId) {
          if (!consequenceMap.hasOwnProperty(type)) {
            consequenceMap[type] = {};
          }

          var c = consequenceMap[type];
          if (!c.hasOwnProperty(geneId)) {
            c[geneId] = {};
            c[geneId].symbol = consequence.geneAffectedSymbol;
            c[geneId].id = geneId;
            c[geneId].aaChangeList = [];
          }
          c[geneId].aaChangeList.push({
            'aaMutation': consequence.aaMutation,
            'FI': consequence.functionalImpact
          });
        }
      });


      // Dump into a list, easier to format/sort
      scope.consequences = [];
      for (var k in consequenceMap) {
        if (consequenceMap.hasOwnProperty(k)) {
          scope.consequences.push({
            consequence: k,
            data: consequenceMap[k]
          });
        }
      }

      var precedence = Consequence.precedence();

      scope.consequences = $filter('orderBy')(scope.consequences, function (t) {
        var index = precedence.indexOf(t.consequence);
        if (index === -1) {
          return precedence.length + 1;
        }
        return index;
      });
    }
  };
});
//Mike
angular.module('icgc.ui.popover', [])
  .directive('popover', function ($sce) {
    return {
      restrict: 'AE',
      transclude: true,
      replace: true,
      scope: {
        'popoverAnchorText': '@popoverAnchorLabel',
        'popoverTitle': '@',
        'assistIconClass': '@popoverAssistIconClass',
        'assistIconPositionBefore': '@popoverAssistIconPositionBefore',
        'isOpen': '=popoverIsOpen',
        'hideDelay': '@popoverHideDelay'
      },
      templateUrl: '/scripts/ui/views/popover.html',
      link: function (scope, element) {

        function Popover(id) {
          var _popoverEl = element.find(id),
              _outerBorder = _popoverEl.find('.popover-inner-container-border'),
              _popoverContent = _popoverEl.find('.popover-inner-container'),
              _self = this,
              _containerHoverCount = 0,
              _timeout_in_ms = scope.hideDelay || 700;

          function _init() {

            _popoverEl.hover(function() {
              _self.show();
            },
            function() {
              _self.hide();
            });

          }

          function _popoverStatusCheck() {
            setTimeout(_popoverShow, _timeout_in_ms);
          }

          function _popoverShow(shouldShow) {

            if (shouldShow === true) {
              _popoverEl.show();
              _outerBorder.fadeIn('fast');
              _popoverContent.fadeIn('fast');
            }
            else if (_containerHoverCount === 0) {
              _popoverContent.fadeOut('fast');
              _outerBorder.fadeOut('fast', function() {
                _popoverEl.hide();
              });

            }
          }


          _init();


          // Popover Public API
          this.show = function() {
            _containerHoverCount++;

            _popoverShow(true);
            _popoverStatusCheck();
          };

          this.hide = function() {
            _containerHoverCount  = Math.max(0, _containerHoverCount - 1 );
            _popoverStatusCheck();
          };

          this.popoverTimeout = function(timeoutInMS) {

            if (arguments.length > 0) {
              _timeout_in_ms = timeoutInMS;
            }

            return _timeout_in_ms;
          };


        }
      
        function _init() {
          var popover = new Popover('.popover-outer-container');
          scope.title = $sce.trustAsHtml(scope.popoverTitle);
          scope.anchorText = $sce.trustAsHtml(scope.popoverAnchorText); 
          scope.isAssistIconBeforeLabel = scope.assistIconPositionBefore === 'true' ? true : false;


          element.hover(
            function() {
              popover.show();
            },
            function() {
              setTimeout(function() {
                popover.hide();
              }, popover.popoverTimeout());
            }
          );

        }




        _init();
      }
  };
});

angular.module('icgc.ui.copyPaste', [])
  .provider('copyPaste', function () {
    var _provider = this,
        _zeroClipPath = '//cdnjs.cloudflare.com/ajax/libs/zeroclipboard/2.2.0/ZeroClipboard.swf',
        _copyPasteConfig = {};
    
    // Getter/Setter for flash fallback
    _provider.zeroClipboardPath = function (path) {

      if (typeof path !== 'string') {
        return _zeroClipPath;
      }
      
      _zeroClipPath = path;
    }; 
    
    _provider.config = function (config) {
      
      if (! angular.isObject(config)) {
        return _copyPasteConfig;
      }
      
      _copyPasteConfig = config;
    };
    
    
    _provider.$get = function () {
      return _provider;
    };
     
  })
  .run(function (copyPaste) {
    
    if (! angular.isDefined(window.ZeroClipboard) ) {
     console.warn('The copyPaste module depends on ZeroClipboard Version 2.2.0+.' +
        '\nPlease include this dependency!');
     return;
    }
    
    var zeroClipboardPathConfig = {
        swfPath: copyPaste.zeroClipboardPath(),
        trustedDomains: ['*'],
        allowScriptAccess: 'always',
        forceHandCursor: true,
        debug: true
    };
    
    // Configure ZeroClipboard in case we need to use it later.
    ZeroClipboard.config(angular.extend(zeroClipboardPathConfig, copyPaste.config));
  })
  .directive('copyToClip', function ($document, gettextCatalog) {

        return {
          restrict: 'A',
          transclude: true,
          template: '<div class="copy-to-clip-container">' +
                      '<div class="copy-to-clip-message-container">' +
                        '<div class="copy-to-clip-message-content"></div>' +
                        '<div class="arrow"></div>' +
                      '</div>' +
                      '<div class="copy-to-clip-content" data-ng-transclude></div>' +
                    '</div>',         
            scope: {
                onCopy: '&',
                onError: '&',
                copyData: '=',
                onCopyFocusOn: '@',
                onCopySuccessMessage: '@',
                onHoverMessage: '@'
            },
            link: function (scope, element, attrs) {

             function _selectText() {

                var textEl = _focusOnCopySelector[0],
                    range = null;

                if (window.getSelection) {  // The cool browsers...
                  var selection = window.getSelection();
                  range = _document.createRange();
                  range.selectNodeContents(textEl);
                  selection.removeAllRanges();
                  selection.addRange(range);
                }
                else if (_document.body.createTextRange) { // Fail... M$
                  range = _document.body.createTextRange();
                  range.moveToElementText(textEl);
                  range.select();
                }

             }
              
             function _focusCopyEl() {
               if (! _focusOnCopySelector || _focusOnCopySelector.length === 0) {
                 return;
               }

               _focusOnCopySelector.focus();

              if (_focusOnCopySelector.is(':input')) {
                _focusOnCopySelector[0].setSelectionRange(0, _focusOnCopySelector.val().length);
              }
              else {
                _selectText();
              }

             }

              function _hideTipMessage() {
                if (!_showCopyTips || _showingCopiedStatusMessage) {
                  return;
                }

                _messageBubble.fadeOut('fast');
              }

              function _showTipMessage(isSuccess, overrideMessage, timeoutInMS) {

                // Status copy messages take priority over other messages
                if (!_showCopyTips || _showingCopiedStatusMessage) {
                  return;
                }
                
                var msg = '',
                    copyPasteCommandKey = 'Ctrl',
                    copyCommandAlphaKey = 'c',
                    pasteCommandAlphaKey = 'v';  
                    
                
                
                if (typeof overrideMessage === 'string') { // We are using this as a tooltip/status notifier
                  msg = overrideMessage;
                }
                else { // Or else we handle success/failure of copy
                  switch (_browserOSPlatform) {
                    case 'mac':
                      copyPasteCommandKey = '&#x2318;';
                      break;
                    default:
                      break;
                  }

                  _showingCopiedStatusMessage = true;

                  if (isSuccess) {
                    msg = scope.onCopySuccessMessage ? scope.onCopySuccessMessage : '';


                    if (! msg) {
                      msg += 'Press ' + copyPasteCommandKey +
                             '-' + pasteCommandAlphaKey + ' to paste.';
                    }
                  }
                  else {
                    msg = 'Press' + copyPasteCommandKey + '-' + copyCommandAlphaKey +
                    ' to copy and ' + copyPasteCommandKey + '-' +
                    pasteCommandAlphaKey + ' to paste.';
                  }
                }
               
                _messageConfirmationBody.html(msg);
                  
                _messageBubble.css({ 
                    top: - (_messageBubble.outerHeight() + 3),
                    left: _targetElement.outerWidth()/2 - (_messageBubble.outerWidth()/2)
                });

                _messageBubble.fadeIn('fast');
              
              
                if (_previousMessageTimeout !== null) {
                  clearTimeout(_previousMessageTimeout);
                }
              
                _previousMessageTimeout = setTimeout(function () {
                  _messageBubble.fadeOut('fast');
                  _previousMessageTimeout = null;
                  _showingCopiedStatusMessage = false;
                }, timeoutInMS || 2500);
              }
              
              //
              function _createTextArea(text) {

                if (!_textArea) {
                  _textArea = _document.createElement('textarea');
                  _textArea.style.position = 'absolute';
                  _textArea.style.top = '-10000px';
                  _textArea.style.left = '0px';
                  _documentBody.appendChild(_textArea);
                }  
                  
                _textArea.textContent = text;

                return _textArea;
              }
                
          
              //
              function _copyTextArea(textArea) {
                   
                  // Set inline style to override css styles
                  _documentBody.style.webkitUserSelect = 'initial';

                  var selection = _document.getSelection();
                  selection.removeAllRanges();
                  textArea.select();

                  if (! _document.execCommand('copy')) {
                      throw new Error('Failure to perform copy using built-in copy-and-paste!');
                  }
                  
                  selection.removeAllRanges();

                  // Reset inline style
                  _documentBody.style.webkitUserSelect = '';
              }
                
              //
              function _initCopyZeroClipboard() {

                if (_zeroClipBoardClient !== null) {
                  return;     
                }

                var isNativeCopySupported = false;
               
                _zeroClipBoardClient = new ZeroClipboard(_targetElement);
                
                _zeroClipBoardClient.on({
                  'copy': function (e) {
                    e.clipboardData.setData(_dataMimeType, scope.copyData);
                    _focusCopyEl();
                  },
                  'beforecopy': function () {
                    // In our listener look to see if we can use the native copy
                    if (_testNativeCopy()) {
                      isNativeCopySupported = true;
                    }
                  },
                  'aftercopy': function () { 
                    // Execute this in the angular context...
                    scope.$apply(scope.onCopy);
                    _showTipMessage(true, null, 1300);
                    
                    // After the first copy if native copy is supported
                    // kill this client
                    if (isNativeCopySupported) {
                      setTimeout(function () { 
                        _zeroClipBoardClient.destroy();
                        _zeroClipBoardClient = null;
                        _initNativeCopyClipboard();
                      }, 10);
                    }                 
                  },
                  'ready': function () {
                    // We are good to go!
                    
                  },
                  'error': function (e) {
                    // Something bad happened
                     if (scope.onError) {
                        scope.onError({error: e});
                     }
                  }

                });
          
              }
              
              function _initNativeCopyClipboard() {
                _targetElement.on('click', function (event) {
                    
                    event.stopPropagation();

                    try {
                      _copyText(scope.copyData);

                      _focusCopyEl();

                      if (scope.onCopy) {
                        scope.onCopy();
                      }

                      _showTipMessage(true, null, 1300);

                    }
                    catch (e) {

                      _showTipMessage(false);

                      if (scope.onError) {
                        scope.onError({ error: e });
                      }
                    }

                  });
              }
                
              //
              function _copyText(text) {

                try {
                  _copyTextArea(_createTextArea(text));
                }
                catch (e) {
                  // Remove the textarea we just created since we can't reuse it/
                  _documentBody.removeChild(_textArea);
                  _textArea = null;
                  
                  throw new Error('Failure to perform copy using native copy command!');
                  
                }
                  
              }
              
              //
              function _testNativeCopy() {
                var isNativeCopySupported = true;
                
                try {
                  _copyTextArea(_createTextArea('.'));
                }
                catch (e) {
                  isNativeCopySupported = false;
                }
                
                return isNativeCopySupported;
              }
              
              //
              function _init() {
 
                ////////////////////////////////////////////////////////////////
                // Copy strategy
                ////////////////////////////////////////////////////////////////
                // 1. Load ZeroClipboard flash if it's supported. If native
                //    copy is supported on the first copy it will fallback to 
                //    it and destroy the flash movie being used. If not it will
                //    continue to use it.
                //
                // 2. If flash is not supported it will try to use the native 
                //    copy. If the native copy fails it will call the optional
                //    onError method which can be passed into the directive
                //    from a parent scope (controller).
                ////////////////////////////////////////////////////////////////

                element.addClass('copy-to-clip');

                if (scope.onHoverMessage) {
                  element.hover(
                    function() {
                      _showTipMessage(null, scope.onHoverMessage, 1500);
                    },
                    function() {
                      _hideTipMessage();
                    }
                  );
                }
                
                if (_zeroClipboardSupported) {
                  _initCopyZeroClipboard();
                }
                else {
                  _initNativeCopyClipboard();
                }
                
                 if (_promptOnCopy) {
                  _showTipMessage(null, gettextCatalog.getString('Click here to copy to your clipboard.'));
                }
                
                // Destroy the ZeroClipboard Client if it exists...and remove listeners
                scope.$on('$destroy', function () {
                  if (_zeroClipBoardClient !== null) {
                    _zeroClipBoardClient.destroy();
                  }
                  
                  // Cleanup the textarea if it's there...
                  if (_textArea !== null) {
                    _documentBody.removeChild(_textArea);
                  }
                  
                  _targetElement.unbind('click');
                });
              }
              
              
              var _zeroClipBoardClient = null,
                // Do some tests to see if we have a chance with Flash
                _zeroClipboardSupported = !ZeroClipboard.isFlashUnusable(),
                _document = $document[0],
                _dataMimeType = attrs.mimeType || 'text/plain',
                _documentBody = _document.body,
                _textArea = null,
                _showCopyTips = attrs.showCopyTips === 'false' ? false : true,
                _promptOnCopy = attrs.promptOnCopy === 'true' ? true : false,
                _targetElement = element.find('.copy-to-clip-content'),
                _messageConfirmationBody = element.find('.copy-to-clip-message-content'),
                _messageBubble = element.find('.copy-to-clip-message-container'),
                _focusOnCopySelector = scope.onCopyFocusOn ? element.find(scope.onCopyFocusOn) : false,
                _previousMessageTimeout = null,
                _showingCopiedStatusMessage = false,
                _browserOSPlatform = window.navigator.platform ?
                  (navigator.platform.toLowerCase().indexOf('mac') >= 0 ? 'mac' : 'win') : 'win';
              
            
              _init(); 
                
            }
        };
    });
/**
 * Used in keyword search, should rename - DC
 */
angular.module('app.ui.toggle', []).directive('toggleParam', function (LocationService) {
  return {
    restrict: 'A',
    transclude: true,
    replace: true,
    scope: {
      key: '@',
      value: '@',
      defaultSelect: '@'
    },
    template: '<span data-ng-class="{\'t_labels__label_inactive\': !active}" data-ng-click="update()">' +
              '<i data-ng-class="{\'icon-ok\': active}"></i> {{value | readable}}</span>',
    link: function (scope) {
      var type = LocationService.search()[scope.key];

      if (type) {
        scope.active = type === scope.value;
      } else {
        scope.active = !!scope.defaultSelect;
      }

      scope.update = function () {
        LocationService.setParam(scope.key, scope.value);
      };

      scope.$watch(function () {
        return LocationService.search()[scope.key];
      }, function (n) {
        if (n) {
          scope.active = n === scope.value;
        } else {
          scope.active = !!scope.defaultSelect;
        }
      }, true);
    }
  };
});

angular.module('icgc.ui.badges', []).directive('pcawgBadge', function () {
  return {
    restrict: 'E',
    replace: true,
    template: '<span class="badge pcawg-badge">PCAWG</span>'
  };
})
  .directive('studyBadge', function () {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        study: '@',
        text: '@'
      },
      template: '<div><div><pcawg-badge data-ng-if="isPcawg"></div>' +
      '<div><span data-ng-if="!isPcawg">{{ text }}</span></div></div>',
      link: function (scope) {
        scope.isPcawg = (scope.study || '').toUpperCase() === 'PCAWG';
        // Displays the value of the 'study' if the 'text' attribute is not provided (i.e. no override supplied).
        scope.text = scope.text || scope.study;
      }
    };
  });

angular.module('icgc.ui.iobio', [])
  .component('iobioStatistics', {
      templateUrl: 'scripts/ui/views/iobio-statistics.html',
      bindings: {
        fileCopies: '=',
        objectId: '=',
        rowId: '='
      },
      controller: function($modal, $rootScope){
        var _ctrl = this;

        function uniquelyConcat (fileCopies, property) {
          return _(fileCopies)
            .map (property)
            .unique()
            .join(', ');
        }

        _ctrl.fileFormats = function (fileCopies) {
          return uniquelyConcat (fileCopies, 'fileFormat');
        };

        _ctrl.awsOrCollab = function(fileCopies) {
          return _.includes(_.pluck(fileCopies, 'repoCode'), 'aws-virginia') ||
            _.includes(_.pluck(fileCopies, 'repoCode'), 'collaboratory');
        };

        _ctrl.showIobioModal = function(objectId, objectName, name) {
          var fileObjectId = objectId;
          var fileObjectName = objectName;
          var fileName = name;
          $modal.open ({
            controller: 'ExternalIobioController',
            template: '<section id="bam-statistics" class="bam-statistics-modal">'+
              '<bamstats bam-id="bamId" on-modal=true bam-name="bamName" bam-file-name="bamFileName" data-ng-if="bamId">'+
              '</bamstats></section>',
            windowClass: 'bam-iobio-modal',
            resolve: {
              params: function() {
                return {
                  fileObjectId: fileObjectId,
                  fileObjectName: fileObjectName,
                  fileName: fileName
                };
              }
            }
          }).opened.then(function() {
            setTimeout(function() { $rootScope.$broadcast('bamready.event', {})}, 300);

          });
        };

        _ctrl.showVcfIobioModal = function(objectId, objectName, name) {
          var fileObjectId = objectId;
          var fileObjectName = objectName;
          var fileName = name;
          $modal.open ({
            controller: 'ExternalVcfIobioController',
            template: '<section id="vcf-statistics" class="vcf-statistics-modal">'+
              '<vcfstats vcf-id="vcfId" on-modal=true vcf-name="vcfName" vcf-file-name="vcfFileName" data-ng-if="vcfId">'+
              '</vcfstats></section>',
            windowClass: 'vcf-iobio-modal',
            resolve: {
              params: function() {
                return {
                  fileObjectId: fileObjectId,
                  fileObjectName: fileObjectName,
                  fileName: fileName
                };
              }
            }
          }).opened.then(function() {
            setTimeout(function() { $rootScope.$broadcast('bamready.event', {})}, 300);
          });
        };

        _ctrl.getAwsOrCollabFileName = function(fileCopies) {
          try {
            var fCopies = _.filter(fileCopies, function(fCopy) {
              return fCopy.repoCode === 'aws-virginia' || fCopy.repoCode === 'collaboratory';
            });

            return _.pluck(fCopies, 'fileName')[0];
          } catch (err) {
            console.error(err);
            return 'Could Not Retrieve File Name';
          }
        };

      }
  });

angular.module('icgc.ui.loader', [])
  .component('loadingBlock', {
    template: 
    `<span class="loading-block">
        {{ ['&#9724;&#9724;&#9724;','&#9724;&#9724;&#9724;', '&#9724;&#9724;', '&#9724;'] | _:'sample' }}
      </span>`,
      replace: true
  });

angular.module('icgc.ui.splitButtons', [])
  .component('entitySetFacet', {
    templateUrl: '/scripts/ui/views/entity-set-facet.html',
    bindings: {
      entityType: '@',
      entitySet: '=',
      clickEvent: '<',
      selectEvent: '<'
    },
    replace: true
  });