/*-
 * #%L
 * dEF-Pi dashboard gateway
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
function widget(updateMethod, timeout, callback) {
	me = this;
	this.callback = callback;

	this.error = function(msg) { /* Do nothing by default */ };
	this.update = function() {
		$.ajax(updateMethod, {
			dataType: "json",
			success: function(data) {
				me.callback(data);
				window.setTimeout(me.update, timeout);
			},
			error: function(msg) {
				me.error(msg.statusText);
			}
		});
	};
	this.update();

	this.call = function(method, data, callback) {
		$.ajax(method, {
			type: "POST",
			dataType: "json",
			data: JSON.stringify(data),
			success: callback
		});
	};
}
