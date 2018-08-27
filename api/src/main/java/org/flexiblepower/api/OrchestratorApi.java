/*-
 * #%L
 * dEF-Pi API
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
package org.flexiblepower.api;

/**
 * OrchestratorApi
 *
 * @version 0.1
 * @since Apr 10, 2017
 */
public interface OrchestratorApi {

    /**
     * The name of the user authentication domain
     */
    public final static String USER_AUTHENTICATION = "UserSecurity";

    /**
     * The name of the administrator authentication domain
     */
    public final static String ADMIN_AUTHENTICATION = "AdminSecurity";

    /**
     * The default maximum number of items on a page in the UI
     */
    public final static String DEFAULT_ITEMS_PER_PAGE = "1000";

}
