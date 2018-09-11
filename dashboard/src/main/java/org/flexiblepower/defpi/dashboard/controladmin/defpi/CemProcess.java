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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CemProcess extends DefPiProcess {

    private final List<String> rmProcessIds = new ArrayList<>();

    public CemProcess(final DefPiConnectionAdmin defPiConnectionAdmin, final String processId, final String serviceId) {
        super(defPiConnectionAdmin, processId, serviceId);
    }

    public List<String> getRmProcessIds() {
        return this.rmProcessIds;
    }

    public List<RmProcess> getConnectedRmProcess() {
        return this.rmProcessIds.stream().map(id -> this.connectionAdmin.getRmProcess(id)).collect(Collectors.toList());
    }

    public boolean isConnectedWith(final RmProcess rmProcess) {
        for (final String id : this.rmProcessIds) {
            if (id.equals(rmProcess.getProcessId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "CemProcess [rmProcessId=" + this.rmProcessIds + ", getProcessId()=" + this.getProcessId()
                + ", getServiceId()=" + this.getServiceId() + "]";
    }

}
