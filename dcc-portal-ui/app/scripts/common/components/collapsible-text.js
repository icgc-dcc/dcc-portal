import insertCss from 'insert-css';

angular
.module('app.common.components')
.directive('collapsibleText', function () {
  insertCss(`
    .collapsible-text--collapsed {
      text-overflow: ellipsis;
      white-space: nowrap !important;
      overflow: hidden !important;
    }
  `);
  return {
    restrict: 'AE',
    link: function (scope, $element) {
      $element.addClass('collapsible-text collapsible-text--collapsed')
      $element.click(() => $element.toggleClass('collapsible-text--expanded collapsible-text--collapsed'));
    }
  };
});
