'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});
var location = function location($locationProvider) {
    // Keep the start of all routes to #/ instead of #!/
    // while updating to Angular 1.6
    // @see https://docs.angularjs.org/guide/migration#commit-aa077e8
    $locationProvider.hashPrefix('');
};

location.$inject = ['$locationProvider'];

exports.default = location;
module.exports = exports['default'];
//# sourceMappingURL=location.js.map