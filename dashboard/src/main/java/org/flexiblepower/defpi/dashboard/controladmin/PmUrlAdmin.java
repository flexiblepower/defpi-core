package org.flexiblepower.defpi.dashboard.controladmin;

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
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.flexiblepower.api.ProcessApi;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Process.ProcessParameter;
import org.flexiblepower.service.DefPiParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public class PmUrlAdmin {

    private static final String POWERMACHER_URL_PARAM_KEY = "webSocketUrl";
    private static final String POWERMATCHER_SERVICE_ID = "powermatcher_defpi";

    public static final Logger LOG = LoggerFactory.getLogger(PmUrlAdmin.class);

    private final ProcessApi processApi;
    private final DefPiParameters parameters;

    public PmUrlAdmin(final DefPiParameters parameters) {
        this.parameters = parameters;
        this.processApi = JAXRSClientFactory.create("http://" + PmUrlAdmin.stripURI(parameters.getOrchestratorHost())
                + ":" + parameters.getOrchestratorPort(), ProcessApi.class, Arrays.asList(new JacksonJsonProvider()));
        WebClient.client(this.processApi).header("X-Auth-Token", this.parameters.getOrchestratorToken());
    }

    public Process getPmProcess() throws AuthorizationException {
        for (final Process p : this.processApi
                .listProcesses(1, 1000, "ASC", "id", "{\"userId\": \"" + this.parameters.getUserId() + "\"}")) {
            if (PmUrlAdmin.POWERMATCHER_SERVICE_ID.equals(p.getServiceId())) {
                // We assume there is only 1 PM per user
                return p;
            }
        }
        return null;
    }

    public String getPmUrl() throws AuthorizationException {
        final Process pmProcess = this.getPmProcess();
        if (pmProcess == null) {
            return null;
        }
        final List<ProcessParameter> configuration = pmProcess.getConfiguration();
        if (configuration == null) {
            return null;
        }
        for (final ProcessParameter p : configuration) {
            if (PmUrlAdmin.POWERMACHER_URL_PARAM_KEY.equals(p.getKey())) {
                return p.getValue();
            }
        }
        return null;
    }

    public boolean setPmUrl(final String newUrl) throws AuthorizationException,
            InvalidObjectIdException,
            ProcessNotFoundException {
        final Process pmProcess = this.getPmProcess();
        if (pmProcess == null) {
            return false;
        }
        final List<ProcessParameter> newConfiguration = new ArrayList<>();
        // copy all with the exception of the url
        final List<ProcessParameter> configuration = pmProcess.getConfiguration();
        if (configuration != null) {
            for (final ProcessParameter p : configuration) {
                if (!PmUrlAdmin.POWERMACHER_URL_PARAM_KEY.equals(p.getKey())) {
                    newConfiguration.add(p);
                }
            }
        }
        // set the url
        newConfiguration.add(new ProcessParameter(PmUrlAdmin.POWERMACHER_URL_PARAM_KEY, newUrl));
        pmProcess.setConfiguration(newConfiguration);
        // submit
        this.processApi.updateProcess(pmProcess.getId().toString(), pmProcess);
        PmUrlAdmin.LOG.info("Changed PowerMatcher url to " + newUrl);
        return true;
    }

    private static String stripURI(final String uri) {
        int begin = 0;
        int end = uri.length();
        if (uri.startsWith("/")) {
            begin = 1;
            end = uri.length();
        }
        if (uri.endsWith("/")) {
            end = Math.max(uri.length() - 1, 1);
        }
        return uri.substring(begin, end);
    }

}
