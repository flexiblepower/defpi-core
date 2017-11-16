/**
 * File Service.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flexiblepower.service;

import java.io.Serializable;

/**
 * Service
 *
 * The class implementing the service *must* have a no-args constructor
 *
 * @version 0.1
 * @since Apr 24, 2017
 */
public interface Service<T> {

    /**
     * This function is only called if this instance is the resumed version of an old process. It is called with the
     * serialized process state, which was returned from the {@link #suspend()} function. This function is called
     * *before* the {@link #init(T, DefPiParameters)}) function, if it is a resumed instance of an earlier process.
     *
     * @param state
     */
    public void resumeFrom(Serializable state);

    /**
     * This function is called after the constructor (or immediately after the {@link #resumeFrom(Serializable)} if
     * applicable), when the configuration is first available. This method is only called once, and after it, the
     * service is considered to be "running".
     *
     * @see #modify(T)
     * @param config
     * @param parameters
     */
    public void init(T config, DefPiParameters parameters);

    /**
     * This function is called when the configuration changes during runtime. It may be called multiple times.
     *
     * @see #init(T, DefPiParameters)
     * @param config
     */
    public void modify(T config);

    /**
     * Marks that this process is about to be suspended. This means the object *will* be destroyed, and may be
     * subsequently created in another iteration. Any data has to be stored now.
     *
     * @return serialised state of the service
     */
    public Serializable suspend();

    /**
     * Marks that this process is about to be terminated. This means the object *will* be destroyed.
     */
    public void terminate();

}
