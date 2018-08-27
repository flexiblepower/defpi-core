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

public class RmProcess extends DefPiProcess {

    private String cemProcessId;

    public RmProcess(final DefPiConnectionAdmin defPiConnectionAdmin, final String processId, final String serviceId) {
        super(defPiConnectionAdmin, processId, serviceId);
    }

    public String getCemProcessId() {
        return this.cemProcessId;
    }

    public void setCemProcessId(final String cemProcessId) {
        this.cemProcessId = cemProcessId;
    }

    public CemProcess getConnectedCemProcess() {
        if (this.cemProcessId == null) {
            return null;
        }
        return this.connectionAdmin.getCemProcess(this.cemProcessId);

    }

    public boolean isConnectedWith(final CemProcess cemProcess) {
        return (this.cemProcessId != null) && this.cemProcessId.equals(cemProcess.getProcessId());
    }

    @Override
    public String toString() {
        return "RmProcess [cemProcessId=" + this.cemProcessId + ", getProcessId()=" + this.getProcessId()
                + ", getServiceId()=" + this.getServiceId() + "]";
    }

}
