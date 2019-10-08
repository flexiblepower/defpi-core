package org.flexiblepower.defpi.dashboard.controladmin.defpi;

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

public abstract class DefPiProcess {

    protected DefPiConnectionAdmin connectionAdmin;
    private final String processId;
    private final String serviceId;

    public DefPiProcess(final DefPiConnectionAdmin defPiConnectionAdmin,
            final String processId,
            final String serviceId) {
        this.connectionAdmin = defPiConnectionAdmin;
        this.processId = processId;
        this.serviceId = serviceId;
    }

    public String getProcessId() {
        return this.processId;
    }

    public String getServiceId() {
        return this.serviceId;
    }

}
