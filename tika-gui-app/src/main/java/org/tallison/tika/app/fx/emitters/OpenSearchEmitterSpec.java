/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tallison.tika.app.fx.emitters;

import static org.tallison.tika.app.fx.utils.OptionalUtil.isEmpty;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.tallison.tika.app.fx.ctx.AppContext;
import org.tallison.tika.app.fx.metadata.MetadataTuple;
import org.tallison.tika.app.fx.sax.DomWriter;
import org.w3c.dom.Element;

import org.apache.tika.utils.ProcessUtils;
import org.apache.tika.utils.StringUtils;

public class OpenSearchEmitterSpec extends BaseEmitterSpec {
    private static final String EMITTER_CLASS =
            "org.apache.tika.pipes.emitter.opensearch.OpenSearchEmitter";

    //opensearch
    private static final String OPEN_SEARCH_URL = "openSearchUrl";
    private static final String OPEN_SEARCH_UPDATE_STRATEGY = "updateStrategy";

    private Optional<String> openSearchUrl = Optional.empty();
    private Optional<String> index = Optional.empty();
    private Optional<String> userName = Optional.empty();
    private Optional<String> password = Optional.empty();
    private String updateStrategy = "Upsert";//or Overwrite

    public OpenSearchEmitterSpec(@JsonProperty("metadataTuples") List<MetadataTuple> metadataTuples) {
        super(EMITTER_CLASS, metadataTuples);
    }

    @Override
    public ValidationResult validate() {

        if (isEmpty(openSearchUrl)) {
            return new ValidationResult(ValidationResult.VALIDITY.NOT_OK,
                    "Emitter", "Missing URL?", "Must specify a url including the index, " +
                            "e.g. https://localhost:9200/my-index");
        }

        if (isEmpty(index)) {
            return new ValidationResult(ValidationResult.VALIDITY.NOT_OK,
                    "Emitter", "Missing index?",
                    "Please specify an index, I only see: " + openSearchUrl.get());

        }

        if (isEmpty(password) && ! isEmpty(userName)) {
            return new ValidationResult(ValidationResult.VALIDITY.NOT_OK,
                    "Emitter", "Credentials?", "Username with no password?!");
        }


        if (isEmpty(userName) && ! isEmpty(password)) {
            return new ValidationResult(ValidationResult.VALIDITY.NOT_OK,
                    "Emitter", "Credentials?", "Password with no username?!");
        }

        ValidationResult result = validateMetadataRows();
        if (result.getValidity() == ValidationResult.VALIDITY.OK) {
            valid = true;
        }
        return result;
    }

    @Override
    public ValidationResult initialize() {
        //TODO -- fix!
        return ValidationResult.OK;
    }

    @Override
    public void write(DomWriter writer, Element properties) {
        Element emitters = writer.createAndGetElement(properties, "emitters");
        Element emitterElement = writer.createAndGetElement(emitters, "emitter", "class",
                "org.apache.tika.pipes.emitter.opensearch.OpenSearchEmitter");
        Element params = writer.createAndGetElement(emitterElement, "params");
        writer.appendTextElement(params, "name", "emitter");
        writer.appendTextElement(params, "idField", "_id");

        writer.appendTextElement(params, OPEN_SEARCH_URL, openSearchUrl.get());
        writer.appendTextElement(params, OPEN_SEARCH_UPDATE_STRATEGY, updateStrategy);
        //for now, we need this for upserts.  parent+child upserts are not
        //yet supported in Tika.
        writer.appendTextElement(params, "attachmentStrategy", "SEPARATE_DOCUMENTS");
        writer.appendTextElement(params, "connectionTimeout", "60000");
        writer.appendTextElement(params, "socketTimeout", "120000");

        if (userName.isPresent() && password.isPresent()) {
            writer.appendTextElement(params, "userName", userName.get());
            writer.appendTextElement(params, "password", password.get());
        }
    }

    @Override
    public Set<String> getClassPathDependencies() {
        Set<String> items = new HashSet<>();
        items.add(
                ProcessUtils.escapeCommandLine(
                        AppContext.TIKA_LIB_PATH.resolve("tika-emitter-opensearch")
                                .toAbsolutePath() + "/*"));

        return items;
    }


    public void setUrl(String url) {
        this.openSearchUrl = Optional.of(url);
    }

    public Optional<String> getUrl() {
        return openSearchUrl;
    }

    public void setIndex(String index) {
        this.index = Optional.of(index);
    }

    public void setUpdateStrategy(String updateStrategy) {
        //TODO validate upsert or ...?
        this.updateStrategy = updateStrategy;
    }

    public Optional<String> getIndex() {
        return index;
    }

    public void setUserName(String userName) {
        this.userName = Optional.of(userName);
    }

    public Optional<String> getUserName() {
        return userName;
    }

    public void setPassword(String password) {
        this.password = Optional.of(password);
    }

    public Optional<String> getPassword() {
        return password;
    }

    public String getUpdateStrategy() {
        return updateStrategy;
    }
}
