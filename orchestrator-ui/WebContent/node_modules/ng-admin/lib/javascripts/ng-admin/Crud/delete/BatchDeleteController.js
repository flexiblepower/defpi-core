'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

var BatchDeleteController = function () {
    function BatchDeleteController($scope, $state, $translate, WriteQueries, progression, notification, view, HttpErrorService) {
        _classCallCheck(this, BatchDeleteController);

        this.$scope = $scope;
        this.$state = $state;
        this.$translate = $translate;
        this.WriteQueries = WriteQueries;
        this.progression = progression;
        this.notification = notification;
        this.view = view;
        this.entity = view.getEntity();
        this.entityIds = $state.params.ids;
        this.selection = []; // fixme: query db to get selection
        this.title = view.title();
        this.description = view.description();
        this.actions = view.actions();
        this.loadingPage = false;
        this.fields = view.fields();
        this.HttpErrorService = HttpErrorService;

        $scope.$on('$destroy', this.destroy.bind(this));
    }

    _createClass(BatchDeleteController, [{
        key: 'batchDelete',
        value: function batchDelete($event) {
            var _this = this;

            var entityName = this.entity.name();
            var $translate = this.$translate,
                $state = this.$state,
                progression = this.progression,
                notification = this.notification;

            var fromState = $state.current.name;
            var fromParams = $state.current.params;
            var toState = $state.get('list');
            var toParams = _extends({
                entity: entityName
            }, $state.params);
            progression.start();
            return this.WriteQueries.batchDelete(this.view, this.entityIds).then(function () {
                return $state.go(toState, toParams);
            })
            // no need to call progression.done() in case of success, as it's called by the view dislayed afterwards
            .then(function () {
                return $translate('BATCH_DELETE_SUCCESS');
            }).then(function (text) {
                return notification.log(text, { addnCls: 'humane-flatty-success' });
            }).catch(function (error) {
                progression.done();
                _this.HttpErrorService.handleError($event, toState, toParams, fromState, fromParams, error);
            });
        }
    }, {
        key: 'back',
        value: function back() {
            this.$state.go(this.$state.get('list'), angular.extend({
                entity: this.entity.name()
            }, this.$state.params));
        }
    }, {
        key: 'destroy',
        value: function destroy() {
            this.$scope = undefined;
            this.$state = undefined;
            this.$translate = undefined;
            this.WriteQueries = undefined;
            this.progression = undefined;
            this.notification = undefined;
        }
    }]);

    return BatchDeleteController;
}();

exports.default = BatchDeleteController;


BatchDeleteController.$inject = ['$scope', '$state', '$translate', 'WriteQueries', 'progression', 'notification', 'view', 'HttpErrorService'];
module.exports = exports['default'];
//# sourceMappingURL=BatchDeleteController.js.map