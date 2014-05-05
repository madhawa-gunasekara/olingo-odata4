/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.client.core.communication.request.v4;

import java.net.URI;
import java.util.Collection;
import org.apache.commons.io.IOUtils;
import org.apache.olingo.client.api.communication.header.HeaderName;
import org.apache.olingo.client.api.communication.header.ODataPreferences;
import org.apache.olingo.client.api.communication.request.batch.ODataChangeset;
import org.apache.olingo.client.api.communication.request.batch.ODataRetrieve;
import org.apache.olingo.client.api.communication.request.batch.v4.BatchStreamManager;
import org.apache.olingo.client.api.communication.request.batch.v4.ODataBatchRequest;
import org.apache.olingo.client.api.communication.request.batch.v4.ODataOutsideUpdate;
import org.apache.olingo.client.api.communication.request.v4.AsyncBatchRequestWrapper;
import org.apache.olingo.client.api.communication.response.ODataBatchResponse;
import org.apache.olingo.client.api.communication.response.v4.AsyncResponseWrapper;
import org.apache.olingo.client.api.v4.ODataClient;
import org.apache.olingo.commons.api.edm.constants.ODataServiceVersion;

public class AsyncBatchRequestWrapperImpl extends AsyncRequestWrapperImpl<ODataBatchResponse>
        implements AsyncBatchRequestWrapper {

  private BatchStreamManager batchStreamManager;

  protected AsyncBatchRequestWrapperImpl(final ODataClient odataClient, final ODataBatchRequest odataRequest) {
    super(odataClient, odataRequest);
    batchStreamManager = odataRequest.execute();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ODataChangeset addChangeset() {
    return batchStreamManager.addChangeset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ODataRetrieve addRetrieve() {
    return batchStreamManager.addRetrieve();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ODataOutsideUpdate addOutsideUpdate() {
    return batchStreamManager.addOutsideUpdate();
  }

  @Override
  public AsyncResponseWrapper<ODataBatchResponse> execute() {
    return new AsyncResponseWrapperImpl(batchStreamManager.getResponse());
  }

  public class AsyncResponseWrapperImpl
          extends AsyncRequestWrapperImpl<ODataBatchResponse>.AsyncResponseWrapperImpl {

    /**
     * Constructor.
     *
     * @param res OData batch response.
     */
    public AsyncResponseWrapperImpl(final ODataBatchResponse res) {
      super();

      if (res.getStatusCode() == 202) {
        retrieveMonitorDetails(res);
      } else {
        response = res;
      }
    }

    private void retrieveMonitorDetails(final ODataBatchResponse res) {
      Collection<String> headers = res.getHeader(HeaderName.location.toString());
      if (headers == null || headers.isEmpty()) {
        throw new AsyncRequestException("Invalid async request response. Monitor URL not found");
      } else {
        this.location = URI.create(headers.iterator().next());
      }

      headers = res.getHeader(HeaderName.retryAfter.toString());
      if (headers != null && !headers.isEmpty()) {
        this.retryAfter = Integer.parseInt(headers.iterator().next());
      }

      headers = res.getHeader(HeaderName.preferenceApplied.toString());
      if (headers != null && !headers.isEmpty()) {
        for (String header : headers) {
          if (header.equalsIgnoreCase(new ODataPreferences(ODataServiceVersion.V40).respondAsync())) {
            preferenceApplied = true;
          }
        }
      }

      IOUtils.closeQuietly(res.getRawResponse());
    }
  }
}