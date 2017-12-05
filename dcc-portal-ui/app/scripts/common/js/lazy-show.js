export default ['$animate', function ($animate) {

  return {
    multiElement: true,
    transclude: 'element',
    priority: 600,
    terminal: true,
    restrict: 'A',
    link: function ($scope, $element, $attr, $ctrl, $transclude) {
      var loaded;
      $scope.$watch($attr.ngLazyShow, function ngLazyShowWatchAction(value) {
        if (loaded) {
          $animate[value ? 'removeClass' : 'addClass']($element, 'ng-hide');
        }
        else if (value) {
          loaded = true;
          $transclude(function (clone) {
            clone[clone.length++] = document.createComment(' end ngLazyShow: ' + $attr.ngLazyShow + ' ');
            $animate.enter(clone, $element.parent(), $element);
            $element = clone;
          });
        }
      });
    }
  };

}];
