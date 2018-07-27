'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});
exports.default = maDatagridInfinitePagination;

var _angular = require('angular');

var _angular2 = _interopRequireDefault(_angular);

var _lodash = require('lodash');

var _lodash2 = _interopRequireDefault(_lodash);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var isScrollingDown = function isScrollingDown(wheelEvent) {
    if (!wheelEvent) return true;

    return wheelEvent.deltaY > 0;
};

function maDatagridInfinitePagination($window, $document) {
    var body = $document[0].body;

    return {
        restrict: 'E',
        scope: {
            perPage: '@',
            totalItems: '@',
            nextPage: '&',
            options: '='
        },
        link: function link(scope) {
            var offset = 400;
            if (scope.options && scope.options.offset) {
                offset = scope.options.offset;
            }
            scope.processing = false;
            var perPage = parseInt(scope.perPage, 10) || 1;
            var totalItems = parseInt(scope.totalItems, 10);
            var nbPages = Math.ceil(totalItems / perPage) || 1;
            var loadedPages = [];
            var page = 1;

            var loadNextPage = _lodash2.default.debounce(function () {
                if (page >= nbPages) {
                    return;
                }

                page++;

                if (page in loadedPages) {
                    return;
                }

                scope.processing = true;

                loadedPages.push(page);
                scope.nextPage()(page);
                scope.processing = false;
            }, 500, { maxWait: 1000 });

            var isNearBottom = function isNearBottom() {
                return body.offsetHeight - $window.innerHeight - $window.scrollY < offset;
            };

            var shouldLoadNextPage = function shouldLoadNextPage(wheelEvent) {
                return isScrollingDown(wheelEvent) && !scope.processing && isNearBottom();
            };

            var shouldPreloadNextPage = function shouldPreloadNextPage() {
                if (page >= nbPages) {
                    return false;
                }

                var list = document.getElementsByClassName("list-view");
                if (!list.length) {
                    return false;
                }

                var _list$0$getBoundingCl = list[0].getBoundingClientRect(),
                    bottom = _list$0$getBoundingCl.bottom;

                return bottom < $window.innerHeight;
            };

            var handler = function handler(wheelEvent) {
                if (!shouldLoadNextPage(wheelEvent)) {
                    return;
                }
                loadNextPage();
            };

            // Trigger the load only if necessary (as many times as needed)
            // Necessary = the bottom of the table doesn't reach the end of the page
            var shouldPreloadInterval = setInterval(function () {
                if (shouldPreloadNextPage()) {
                    loadNextPage();
                    return;
                }
                clearInterval(shouldPreloadInterval);
            }, 100);

            $window.addEventListener('wheel', handler);
            scope.$on('$destroy', function () {
                $window.removeEventListener('wheel', handler);
                if (shouldPreloadInterval) {
                    clearInterval(shouldPreloadInterval);
                }
            });
        }
    };
}

maDatagridInfinitePagination.$inject = ['$window', '$document'];
module.exports = exports['default'];
//# sourceMappingURL=maDatagridInfinitePagination.js.map