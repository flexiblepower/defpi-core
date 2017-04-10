'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});
exports.default = maDateField;
var valueToRawValue = function valueToRawValue(value) {
    if (value instanceof Date) {
        return value;
    }

    return new Date(value);
};

/**
 * Edition field for a date - a text input with a datepicker.
 *
 * @example <ma-date-field field="field" value="value"></ma-date-field>
 */
function maDateField() {
    return {
        scope: {
            'field': '&',
            'value': '='
        },
        restrict: 'E',
        link: function link(scope, element) {
            var field = scope.field();
            scope.name = field.name();
            scope.rawValue = scope.value == null ? null : valueToRawValue(scope.value);

            scope.$watch('rawValue', function (newRawValue) {
                if (newRawValue === undefined) {
                    // ui bootstrap datepicker set value to undefined
                    // when the value is invalid and null if empty
                    return;
                }
                var newValue = field.parse()(newRawValue);

                if (!angular.equals(scope.value, newValue)) {
                    scope.value = newValue;
                }
            });

            scope.$watch('value', function (newValue, oldValue) {
                if (angular.equals(newValue, oldValue)) {
                    return;
                }

                if (!newValue) {
                    if (scope.rawValue !== null) {
                        scope.rawValue = null;
                    }

                    return;
                }

                var newRawValue = valueToRawValue(scope.value);

                if (!angular.equals(scope.rawValue, newRawValue)) {
                    scope.rawValue = newRawValue;
                }
            });

            scope.format = field.format();
            if (!scope.format) {
                scope.format = field.type() === 'date' ? 'yyyy-MM-dd' : 'yyyy-MM-dd HH:mm:ss';
            }

            scope.v = field.validation();
            scope.isOpen = false;
            var input = element.find('input').eq(0);
            var attributes = field.attributes();
            for (var name in attributes) {
                input.attr(name, attributes[name]);
            }
            scope.toggleDatePicker = function ($event) {
                $event.preventDefault();
                $event.stopPropagation();
                scope.isOpen = !scope.isOpen;
            };
        },
        template: '<div class="input-group datepicker">\n    <input\n        type="text" ng-model="rawValue" id="{{ name }}" name="{{ name }}" class="form-control"\n        uib-datepicker-popup="{{ format }}" is-open="isOpen" ng-required="v.required"\n        close-text="{{ \'CLOSE\' | translate }}" clear-text="{{ \'CLEAR\' | translate }}" current-text="{{ \'CURRENT\' | translate }}"/>\n    <span class="input-group-btn">\n        <button type="button" class="btn btn-default" ng-click="toggleDatePicker($event)">\n            <i class="glyphicon glyphicon-calendar"></i>\n        </button>\n    </span>\n</div>'
    };
}

maDateField.$inject = [];
module.exports = exports['default'];
//# sourceMappingURL=maDateField.js.map