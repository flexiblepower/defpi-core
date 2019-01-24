package org.flexiblepower.defpi.dashboard;

/*-
 * #%L
 * dEF-Pi dashboard
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

public interface Widget extends HttpHandler {

    public static enum Type {
        FULL_WIDGET,
        SMALL_WIDGET,
    }

    /**
     * Name which can be used in the URL. Not used for small widgets.
     *
     * @return the Id of the Widget
     */
    String getWidgetId();

    String getTitle();

    Widget.Type getType();

    String getProcessId();

    String getServiceId();

}
