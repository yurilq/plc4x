/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.apache.plc4x.sandbox.java.s7.actions;

import org.apache.commons.scxml2.ActionExecutionContext;
import org.apache.commons.scxml2.model.Action;
import org.apache.commons.scxml2.model.ParsedValue;
import org.apache.commons.scxml2.model.ParsedValueContainer;
import org.apache.daffodil.japi.DataProcessor;
import org.apache.daffodil.japi.Diagnostic;
import org.apache.daffodil.japi.WithDiagnostics;
import org.slf4j.Logger;

import java.net.Socket;
import java.util.List;

public abstract class BasePlc4xAction extends Action implements ParsedValueContainer {



    private String socketParameterName;
    private ParsedValue message;

    public String getSocketParameterName() {
        return socketParameterName;
    }

    public void setSocketParameterName(String socketParameterName) {
        this.socketParameterName = socketParameterName;
    }

    @Override
    public ParsedValue getParsedValue() {
        return message;
    }

    @Override
    public void setParsedValue(ParsedValue parsedValue) {
        this.message = parsedValue;
    }

    protected abstract Logger getLogger();

    protected Socket getSocket(ActionExecutionContext ctx) {
        return (Socket) ctx.getGlobalContext().get(getSocketParameterName());
    }

    protected DataProcessor getDaffodilDataProcessor(ActionExecutionContext ctx) {
        return (DataProcessor) ctx.getGlobalContext().get("dfdl");
    }

    protected void logDiagnosticInformation(WithDiagnostics withDiagnostics) {
        List<Diagnostic> diags = withDiagnostics.getDiagnostics();
        for (Diagnostic d : diags) {
            getLogger().error(d.getSomeMessage());
        }
    }

}
