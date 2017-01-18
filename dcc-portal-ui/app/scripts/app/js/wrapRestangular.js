/**
 * This was legacy code moved out to make it easier to spot and quarantine
 */
export default function ($provide, $delegate, $q) {
  $provide.decorator('Restangular', ['$delegate', '$q', function ($delegate, $q) {

    var _cancellableRequests = [];

    function _deletePromiseAbortCache(deferredKey) {

      var indexAt = _cancellableRequests.indexOf(deferredKey);

      if (indexAt < 0) {
        return;
      }

      _cancellableRequests.splice(indexAt, 1);
    }

    // Create a wrapped request function that will allow us to create http requests that
    // can timeout when the abort promise is resolved.
    function _createWrappedRequestFunction(restangularObject, requestFunction) {

      if (!angular.isDefined(requestFunction) || !angular.isFunction(requestFunction)) {
        console.warn('Restangular REST function not defined cannot wrap!');
        return false;
      }

      // Function to wrap the call in which removes the abort promise from the queue on resolve/reject
      return function () {

        var deferred = $q.defer(),
          abortDeferred = $q.defer();

        // Save the deferred object so we may cancel it all later
        _cancellableRequests.push(abortDeferred);

        // Add an auxiliary method to cancel an individual request if one exists
        restangularObject.cancelRequest = function () {
          abortDeferred.resolve();
          _deletePromiseAbortCache(abortDeferred);
        };


        restangularObject.withHttpConfig({ timeout: abortDeferred.promise });

        var requestPromise = requestFunction.apply(restangularObject, Array.prototype.slice.call(arguments));

        requestPromise.then(
          function (data) {
            _deletePromiseAbortCache(abortDeferred);
            deferred.resolve(data);
          },
          function (error) {
            _deletePromiseAbortCache(abortDeferred);
            deferred.reject(error);
          }
        );

        return deferred.promise;
      };

    }

    function _createCancelableRequest(restangularCollectionFunction, args) {
      var callingArgs = Array.prototype.slice.call(args),
        /*jshint validthis:true */
        _this = this;



      var restangularObject = restangularCollectionFunction.apply(_this, callingArgs);

      // Wrap the request items
      restangularObject.get = _createWrappedRequestFunction(
        restangularObject, restangularObject.get
      );

      restangularObject.getList = _createWrappedRequestFunction(
        restangularObject, restangularObject.getList
      );
      restangularObject.post = _createWrappedRequestFunction(
        restangularObject, restangularObject.post
      );

      _wrapRestangular(restangularObject);


      return restangularObject;
    }


    function _wrapRequest(fn) {

      return function () {
        return _createCancelableRequest.call(this, fn, arguments);
      };

    }


    function _wrapRequestFunctions(restangularObj) {

      if (!angular.isDefined(restangularObj.one)) {
        return;
      }

      restangularObj.one = _.bind(_wrapRequest(restangularObj.one), restangularObj);
      restangularObj.all = _.bind(_wrapRequest(restangularObj.all), restangularObj);
    }


    function _wrapRestangular(restangularObj) {

      _wrapRequestFunctions(restangularObj);


      if (!angular.isDefined(restangularObj.withHttpConfig)) {
        return;
      }

      var withHttpConfigFn = restangularObj.withHttpConfig;

      // Wrap the config
      restangularObj.withHttpConfig = function () {
        var withHttpConfigRestangularObject = withHttpConfigFn.apply(this, Array.prototype.slice.call(arguments));

        _wrapRequestFunctions(withHttpConfigRestangularObject);

        return withHttpConfigRestangularObject;
      };

    }

    function _init() {
      _wrapRestangular($delegate);
    }

    _init();

    ///////////////

    $delegate.abortAllHTTPRequests = function () {
      var requestUrls = _.keys(_cancellableRequests);
      var abortRequestLength = requestUrls.length;

      for (var i = 0; i < abortRequestLength; i++) {
        var requestURL = requestUrls[i];
        _cancellableRequests[requestURL].resolve();
      }

      // Reset the deferred abort list
      _cancellableRequests.length = 0;
    };

    return $delegate;
  }]);
}
