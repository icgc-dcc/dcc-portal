/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.resource.core;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.model.DownloadDataType.DONOR;
import static org.icgc.dcc.common.core.model.DownloadDataType.SAMPLE;
import static org.icgc.dcc.common.core.model.DownloadDataType.SSM_CONTROLLED;
import static org.icgc.dcc.common.core.model.DownloadDataType.SSM_OPEN;
import static org.icgc.dcc.portal.resource.core.DownloadResource.ANONYMOUS_USER;
import static org.icgc.dcc.portal.test.JsonNodes.$;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.UUID;

import javax.ws.rs.core.Cookie;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.json.Jackson;
import org.icgc.dcc.download.client.impl.HttpDownloadClient;
import org.icgc.dcc.download.core.jwt.JwtService;
import org.icgc.dcc.download.core.model.DownloadFile;
import org.icgc.dcc.download.core.model.DownloadFileType;
import org.icgc.dcc.download.core.model.JobUiInfo;
import org.icgc.dcc.download.core.response.JobResponse;
import org.icgc.dcc.portal.auth.UserAuthProvider;
import org.icgc.dcc.portal.auth.UserAuthenticator;
import org.icgc.dcc.portal.auth.oauth.OAuthClient;
import org.icgc.dcc.portal.config.PortalProperties.CrowdProperties;
import org.icgc.dcc.portal.config.PortalProperties.DownloadProperties;
import org.icgc.dcc.portal.download.JobInfo;
import org.icgc.dcc.portal.mapper.BadRequestExceptionMapper;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.User;
import org.icgc.dcc.portal.service.DonorService;
import org.icgc.dcc.portal.service.ForbiddenAccessException;
import org.icgc.dcc.portal.service.NotFoundException;
import org.icgc.dcc.portal.service.SessionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.sun.jersey.api.client.ClientResponse;
import com.yammer.dropwizard.testing.ResourceTest;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class DownloadResourceTest extends ResourceTest {

  private static final String SERVICE_URL = "http://localhost:9090";
  private static final String USER_ID = "user1";
  private static final String RESOURCE = "/v1/download";

  private final SessionService sessionService = new SessionService();

  @Mock
  private DonorService donorService;
  @Mock
  private HttpDownloadClient downloadClient;
  @Mock
  private OAuthClient oauthClient;
  @Mock
  private DownloadProperties properties;
  @Mock
  private JwtService tokenService;

  private final UUID sessionToken = UUID.randomUUID();
  private final User user = new User(null, sessionToken);

  @Before
  public void setUp() throws Exception {
    user.setDaco(true);
    user.setEmailAddress(USER_ID);
    sessionService.putUser(sessionToken, user);
  }

  @Override
  protected final void setUpResources() {
    when(downloadClient.isServiceAvailable()).thenReturn(true);
    when(properties.getServerUrl()).thenReturn(SERVICE_URL);
    addResource(new DownloadResource(donorService, downloadClient, properties, tokenService));
    addProvider(BadRequestExceptionMapper.class);
    addProvider(new UserAuthProvider(new UserAuthenticator(sessionService, oauthClient), "openid"));
  }

  @Test
  public void testSubmitJob_controlled() throws Exception {
    when(donorService.findIds(any(Query.class))).thenReturn(singleton("DO1"));

    val expectedUiInfo = JobUiInfo.builder()
        .filter("{\"donor\":{\"id\":{\"is\":[\"DO1\"]}}}")
        .uiQueryStr("testUiQueryStr")
        .controlled(true)
        .user(USER_ID)
        .build();

    val expectedDownloadId = "download-id-1";
    when(downloadClient.submitJob(singleton("DO1"), ImmutableSet.of(DONOR, SSM_CONTROLLED), expectedUiInfo))
        .thenReturn(expectedDownloadId);

    val response = client()
        .resource(RESOURCE + "/submit")
        .queryParam("info", "[{\"key\":\"donor\",\"value\":\"TSV\"},{\"key\":\"ssm\",\"value\":\"TSV\"}]")
        .queryParam("uiQueryStr", "testUiQueryStr")
        .queryParam("filters", "{\"donor\":{\"id\":{\"is\":[\"DO1\"]}}}")
        .cookie(new Cookie(CrowdProperties.SESSION_TOKEN_NAME, sessionToken.toString()))
        .get(ClientResponse.class);

    val actualDownloadId = response.getEntity(JobInfo.class);
    assertThat(actualDownloadId.getDownloadId()).isEqualTo(expectedDownloadId);
  }

  @Test
  public void testSubmitJob_open() throws Exception {
    when(donorService.findIds(any(Query.class))).thenReturn(singleton("DO1"));

    val expectedUiInfo = JobUiInfo.builder()
        .filter("{\"donor\":{\"id\":{\"is\":[\"DO1\"]}}}")
        .uiQueryStr("testUiQueryStr")
        .controlled(false)
        .user(ANONYMOUS_USER)
        .build();

    val expectedDownloadId = "download-id-1";
    when(downloadClient.submitJob(singleton("DO1"), ImmutableSet.of(DONOR, SSM_OPEN), expectedUiInfo))
        .thenReturn(expectedDownloadId);

    val response = client()
        .resource(RESOURCE + "/submit")
        .queryParam("info", "[{\"key\":\"donor\",\"value\":\"TSV\"},{\"key\":\"ssm\",\"value\":\"TSV\"}]")
        .queryParam("uiQueryStr", "testUiQueryStr")
        .queryParam("filters", "{\"donor\":{\"id\":{\"is\":[\"DO1\"]}}}")
        .get(ClientResponse.class);

    val actualDownloadId = response.getEntity(JobInfo.class);
    assertThat(actualDownloadId.getDownloadId()).isEqualTo(expectedDownloadId);
  }

  @Test
  public void testGetJobStatus() throws Exception {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setId(jobId);
    jobResponse.setDataType(singleton(DONOR));
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user(ANONYMOUS_USER)
        .controlled(false)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    val response = client()
        .resource(RESOURCE + "/" + jobId + "/status")
        .get(ClientResponse.class);

    val body = response.getEntity(String.class);
    val bodyJson = Jackson.DEFAULT.readValue(body, ArrayNode.class);
    val expectedBody = $("[{downloadId:'jobId1',status:'SUCCEEDED',"
        + "progress:[{dataType:'donor',completed:'true',numerator:'1',denominator:'1',percentage:'1.0'}]}]");
    assertThat(bodyJson).isEqualTo(expectedBody);
  }

  @Test
  public void testGetJobStatus_controlled() throws Exception {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setId(jobId);
    jobResponse.setDataType(singleton(DONOR));
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user(USER_ID)
        .controlled(true)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    val response = client()
        .resource(RESOURCE + "/" + jobId + "/status")
        .cookie(new Cookie(CrowdProperties.SESSION_TOKEN_NAME, sessionToken.toString()))
        .get(ClientResponse.class);

    val body = response.getEntity(String.class);
    val bodyJson = Jackson.DEFAULT.readValue(body, ArrayNode.class);
    val expectedBody = $("[{downloadId:'jobId1',status:'SUCCEEDED',"
        + "progress:[{dataType:'donor',completed:'true',numerator:'1',denominator:'1',percentage:'1.0'}]}]");
    assertThat(bodyJson).isEqualTo(expectedBody);
  }

  @Test(expected = ForbiddenAccessException.class)
  public void testGetJobStatus_controlledUnauthorized() throws Exception {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user(USER_ID)
        .controlled(true)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    client()
        .resource(RESOURCE + "/" + jobId + "/status")
        .get(ClientResponse.class);
  }

  @Test(expected = ForbiddenAccessException.class)
  public void testGetJobStatus_forbidden() throws Exception {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setId(jobId);
    jobResponse.setDataType(singleton(DONOR));
    jobResponse.setJobInfo(JobUiInfo.builder()
        .controlled(true)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    client()
        .resource(RESOURCE + "/" + jobId + "/status")
        .get(ClientResponse.class);
  }

  @Test(expected = ForbiddenAccessException.class)
  public void testGetJobStatus_controlledForbidden() throws Exception {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setId(jobId);
    jobResponse.setDataType(singleton(DONOR));
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user("someOtherUser")
        .controlled(true)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    val response = client()
        .resource(RESOURCE + "/" + jobId + "/status")
        .cookie(new Cookie(CrowdProperties.SESSION_TOKEN_NAME, sessionToken.toString()))
        .get(ClientResponse.class);

    val body = response.getEntity(String.class);
    val bodyJson = Jackson.DEFAULT.readValue(body, ArrayNode.class);
    val expectedBody = $("[{downloadId:'jobId1',status:'SUCCEEDED',"
        + "progress:[{dataType:'donor',completed:'true',numerator:'1',denominator:'1',percentage:'1.0'}]}]");
    assertThat(bodyJson).isEqualTo(expectedBody);
  }

  @Test
  public void testGetDataTypeSizePerFileType_open() throws Exception {
    when(donorService.findIds(any(Query.class))).thenReturn(singleton("DO1"));
    when(downloadClient.getSizes(singleton("DO1")))
        .thenReturn(ImmutableMap.of(DONOR, 1L, SSM_OPEN, 2L, SSM_CONTROLLED, 3L));

    val response = client()
        .resource(RESOURCE + "/size")
        .queryParam("filters", "")
        .get(ClientResponse.class);

    val body = response.getEntity(String.class);
    val bodyJson = Jackson.DEFAULT.readValue(body, ObjectNode.class);
    val expectedBody = $("{fileSize:[{label:'ssm',sizes:2},{label:'donor',sizes:1}]}");
    assertThat(bodyJson).isEqualTo(expectedBody);
  }

  @Test
  public void testGetDataTypeSizePerFileType_controlled() throws Exception {
    when(donorService.findIds(any(Query.class))).thenReturn(singleton("DO1"));
    when(downloadClient.getSizes(singleton("DO1")))
        .thenReturn(ImmutableMap.of(DONOR, 1L, SSM_OPEN, 2L, SSM_CONTROLLED, 3L));

    val response = client()
        .resource(RESOURCE + "/size")
        .queryParam("filters", "")
        .cookie(new Cookie(CrowdProperties.SESSION_TOKEN_NAME, sessionToken.toString()))
        .get(ClientResponse.class);

    val body = response.getEntity(String.class);
    val bodyJson = Jackson.DEFAULT.readValue(body, ObjectNode.class);
    val expectedBody = $("{fileSize:[{label:'ssm',sizes:3},{label:'donor',sizes:1}]}");
    assertThat(bodyJson).isEqualTo(expectedBody);
  }

  @Test
  public void testGetDataTypeSizePerFileType_combineClinical() throws Exception {
    when(donorService.findIds(any(Query.class))).thenReturn(singleton("DO1"));
    when(downloadClient.getSizes(singleton("DO1")))
        .thenReturn(ImmutableMap.of(DONOR, 1L, SAMPLE, 2L, SSM_CONTROLLED, 3L));

    val response = client()
        .resource(RESOURCE + "/size")
        .queryParam("filters", "")
        .get(ClientResponse.class);

    val body = response.getEntity(String.class);
    val bodyJson = Jackson.DEFAULT.readValue(body, ObjectNode.class);
    val expectedBody = $("{fileSize:[{label:'donor',sizes:3}]}");
    assertThat(bodyJson).isEqualTo(expectedBody);
  }

  @Test
  public void testGetJobInfo_open() throws Exception {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setId(jobId);
    jobResponse.setDataType(singleton(DONOR));
    jobResponse.setSubmissionTime(123L);
    jobResponse.setFileSize(543L);
    jobResponse.setJobInfo(JobUiInfo.builder()
        .filter("testFilter")
        .uiQueryStr("testUiQueryStr")
        .user(ANONYMOUS_USER)
        .controlled(false)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    val response = client()
        .resource(RESOURCE + "/" + jobId + "/info")
        .get(ClientResponse.class);

    val body = response.getEntity(String.class);
    val bodyJson = Jackson.DEFAULT.readValue(body, ObjectNode.class);

    log.info("Body: {}", bodyJson);

    val expectedBody = $("{jobId1:{filter:'testFilter',uiQueryStr:'testUiQueryStr',startTime:123,et:123,"
        + "hasEmail:'false','isControlled':'false',status:'FOUND',fileSize:543}}");
    assertThat(bodyJson).isEqualTo(expectedBody);
  }

  @Test
  public void testGetJobInfo_controlled() throws Exception {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setId(jobId);
    jobResponse.setDataType(singleton(DONOR));
    jobResponse.setSubmissionTime(123L);
    jobResponse.setFileSize(543L);
    jobResponse.setJobInfo(JobUiInfo.builder()
        .filter("testFilter")
        .uiQueryStr("testUiQueryStr")
        .user(USER_ID)
        .controlled(true)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    val response = client()
        .resource(RESOURCE + "/" + jobId + "/info")
        .cookie(new Cookie(CrowdProperties.SESSION_TOKEN_NAME, sessionToken.toString()))
        .get(ClientResponse.class);

    val body = response.getEntity(String.class);
    val bodyJson = Jackson.DEFAULT.readValue(body, ObjectNode.class);

    log.info("Body: {}", bodyJson);

    val expectedBody = $("{jobId1:{filter:'testFilter',uiQueryStr:'testUiQueryStr',startTime:123,et:123,"
        + "hasEmail:'false','isControlled':'true',status:'FOUND',fileSize:543}}");
    assertThat(bodyJson).isEqualTo(expectedBody);
  }

  @Test(expected = ForbiddenAccessException.class)
  public void testGetJobInfo_controlledUnauthorized() throws Exception {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user(USER_ID)
        .controlled(true)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    client()
        .resource(RESOURCE + "/" + jobId + "/info")
        .get(ClientResponse.class);

  }

  @Test(expected = ForbiddenAccessException.class)
  public void testGetJobInfo_controlledForbidden() throws Exception {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user("someUser")
        .controlled(true)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    client()
        .resource(RESOURCE + "/" + jobId + "/info")
        .cookie(new Cookie(CrowdProperties.SESSION_TOKEN_NAME, sessionToken.toString()))
        .get(ClientResponse.class);
  }

  @Test
  public void testListDirectory_open() throws Exception {
    val downloadFiles = ImmutableList.of(
        new DownloadFile("/path/ssm.open.TST1-CA.tsv.gz", DownloadFileType.FILE, 1, 1),
        new DownloadFile("/path/ssm.controlled.TST1-CA.tsv.gz", DownloadFileType.FILE, 1, 1));

    when(downloadClient.listFiles("/path")).thenReturn(downloadFiles);

    val response = client()
        .resource(RESOURCE + "/info/path")
        .get(ClientResponse.class);

    val body = response.getEntity(String.class);
    val bodyJson = Jackson.DEFAULT.readValue(body, ArrayNode.class);
    log.info("BODY: {}", body);

    val expectedBody = $("[{name:'/path/ssm.open.TST1-CA.tsv.gz',type:'f',size:1,date:1}]");
    assertThat(bodyJson).isEqualTo(expectedBody);
  }

  @Test
  public void testListDirectory_controlled() throws Exception {
    val downloadFiles = ImmutableList.of(
        new DownloadFile("/path/ssm.open.TST1-CA.tsv.gz", DownloadFileType.FILE, 1, 1),
        new DownloadFile("/path/ssm.controlled.TST1-CA.tsv.gz", DownloadFileType.FILE, 1, 1));

    when(downloadClient.listFiles("/path")).thenReturn(downloadFiles);

    val response = client()
        .resource(RESOURCE + "/info/path")
        .cookie(new Cookie(CrowdProperties.SESSION_TOKEN_NAME, sessionToken.toString()))
        .get(ClientResponse.class);

    val body = response.getEntity(String.class);
    val bodyJson = Jackson.DEFAULT.readValue(body, ArrayNode.class);
    log.info("BODY: {}", body);

    val expectedBody = $("[{name:'/path/ssm.controlled.TST1-CA.tsv.gz',type:'f',size:1,date:1}]");
    assertThat(bodyJson).isEqualTo(expectedBody);
  }

  @Test
  public void testGetStaticArchive_open() throws Exception {
    val path = "/download_path";
    val token = "token1";
    when(tokenService.createToken(path)).thenReturn(token);

    val response = client()
        .resource(RESOURCE).queryParam("fn", path)
        .get(ClientResponse.class);

    val expectedRedirectUri = URI.create(format("%s/downloads/static?token=%s", SERVICE_URL, token));
    assertThat(response.getLocation()).isEqualTo(expectedRedirectUri);
  }

  @Test
  public void testGetStaticArchive_controlled() throws Exception {
    val path = "/download_controlled_path";
    val token = "token1";
    when(tokenService.createToken(path)).thenReturn(token);

    val response = client()
        .resource(RESOURCE).queryParam("fn", path)
        .cookie(new Cookie(CrowdProperties.SESSION_TOKEN_NAME, sessionToken.toString()))
        .get(ClientResponse.class);

    val expectedRedirectUri = URI.create(format("%s/downloads/static?token=%s", SERVICE_URL, token));
    assertThat(response.getLocation()).isEqualTo(expectedRedirectUri);
  }

  @Test(expected = ForbiddenAccessException.class)
  public void testGetStaticArchive_controlledUnauthorized() throws Exception {
    val path = "/download_controlled_path";
    val token = "token1";
    when(tokenService.createToken(path)).thenReturn(token);

    client()
        .resource(RESOURCE).queryParam("fn", path)
        .get(ClientResponse.class);
  }

  @Test(expected = ForbiddenAccessException.class)
  public void testGetStaticArchive_controlledForbidden() throws Exception {
    val path = "/download_controlled_path";
    val token = "token1";
    when(tokenService.createToken(path)).thenReturn(token);

    client()
        .resource(RESOURCE)
        .queryParam("fn", path)
        .get(ClientResponse.class);
  }

  @Test(expected = NotFoundException.class)
  public void testGetFullArchive_notFound() throws Exception {
    client()
        .resource(RESOURCE)
        .path("fake")
        .get(ClientResponse.class);
  }

  @Test
  public void testGetFullArchive_open() throws Exception {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user(ANONYMOUS_USER)
        .controlled(false)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    val token = "token1";
    when(tokenService.createToken(jobId, ANONYMOUS_USER)).thenReturn(token);

    val response = client()
        .resource(RESOURCE)
        .path(jobId)
        .get(ClientResponse.class);

    val expectedRedirectUri = URI.create(format("%s/downloads?token=%s", SERVICE_URL, token));
    assertThat(response.getLocation()).isEqualTo(expectedRedirectUri);
  }

  @Test
  public void testGetFullArchive_controlled() throws Exception {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user(USER_ID)
        .controlled(true)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    val token = "token1";
    when(tokenService.createToken(jobId, USER_ID)).thenReturn(token);

    val response = client()
        .resource(RESOURCE)
        .path(jobId)
        .cookie(new Cookie(CrowdProperties.SESSION_TOKEN_NAME, sessionToken.toString()))
        .get(ClientResponse.class);

    val expectedRedirectUri = URI.create(format("%s/downloads?token=%s", SERVICE_URL, token));
    assertThat(response.getLocation()).isEqualTo(expectedRedirectUri);
  }

  @Test(expected = ForbiddenAccessException.class)
  public void testGetFullArchive_controlledUnauthorized() throws Exception {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user(USER_ID)
        .controlled(true)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    val token = "token1";
    when(tokenService.createToken(jobId, USER_ID)).thenReturn(token);

    client()
        .resource(RESOURCE)
        .path(jobId)
        .get(ClientResponse.class);
  }

  @Test(expected = ForbiddenAccessException.class)
  public void testGetFullArchive_otherUser() throws Exception {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user("otherUser")
        .controlled(true)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    val token = "token1";
    when(tokenService.createToken(jobId, USER_ID)).thenReturn(token);

    client()
        .resource(RESOURCE)
        .path(jobId)
        .cookie(new Cookie(CrowdProperties.SESSION_TOKEN_NAME, sessionToken.toString()))
        .get(ClientResponse.class);
  }

  @Test
  public void testGetIndividualTypeArchive_open() {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setDataType(singleton(DONOR));
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user(ANONYMOUS_USER)
        .controlled(false)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    val token = "token1";
    when(tokenService.createToken(jobId, ANONYMOUS_USER)).thenReturn(token);

    val response = client()
        .resource(RESOURCE)
        .path(jobId)
        .path(DONOR.getId())
        .get(ClientResponse.class);

    val expectedRedirectUri = URI.create(format("%s/downloads?token=%s&type=%s", SERVICE_URL, token, DONOR.getId()));
    assertThat(response.getLocation()).isEqualTo(expectedRedirectUri);
  }

  @Test
  public void testGetIndividualTypeArchive_ssmOpen() throws Exception {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setDataType(singleton(SSM_OPEN));
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user(ANONYMOUS_USER)
        .controlled(false)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    val token = "token1";
    when(tokenService.createToken(jobId, ANONYMOUS_USER)).thenReturn(token);

    val response = client()
        .resource(RESOURCE)
        .path(jobId)
        .path(SSM_OPEN.getCanonicalName())
        .get(ClientResponse.class);

    val expectedRedirectUri = URI.create(format("%s/downloads?token=%s&type=%s", SERVICE_URL, token, SSM_OPEN.getId()));
    assertThat(response.getLocation()).isEqualTo(expectedRedirectUri);
  }

  @Test
  public void testGetIndividualTypeArchive_ssmControlled() throws Exception {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setDataType(singleton(SSM_CONTROLLED));
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user(USER_ID)
        .controlled(true)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    val token = "token1";
    when(tokenService.createToken(jobId, USER_ID)).thenReturn(token);

    val response = client()
        .resource(RESOURCE)
        .path(jobId)
        .path(SSM_CONTROLLED.getCanonicalName())
        .cookie(new Cookie(CrowdProperties.SESSION_TOKEN_NAME, sessionToken.toString()))
        .get(ClientResponse.class);

    val expectedRedirectUri =
        URI.create(format("%s/downloads?token=%s&type=%s", SERVICE_URL, token, SSM_CONTROLLED.getId()));
    assertThat(response.getLocation()).isEqualTo(expectedRedirectUri);
  }

  @Test
  public void testGetIndividualTypeArchive_controlled() {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setDataType(singleton(DONOR));
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user(USER_ID)
        .controlled(true)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    val token = "token1";
    when(tokenService.createToken(jobId, USER_ID)).thenReturn(token);

    val response = client()
        .resource(RESOURCE)
        .path(jobId)
        .path(DONOR.getId())
        .cookie(new Cookie(CrowdProperties.SESSION_TOKEN_NAME, sessionToken.toString()))
        .get(ClientResponse.class);

    val expectedRedirectUri = URI.create(format("%s/downloads?token=%s&type=%s", SERVICE_URL, token, DONOR.getId()));
    assertThat(response.getLocation()).isEqualTo(expectedRedirectUri);
  }

  @Test(expected = ForbiddenAccessException.class)
  public void testGetIndividualTypeArchive_controlledUnauthorized() {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setDataType(singleton(DONOR));
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user(USER_ID)
        .controlled(true)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    client()
        .resource(RESOURCE)
        .path(jobId)
        .path(DONOR.getId())
        .get(ClientResponse.class);

  }

  @Test(expected = ForbiddenAccessException.class)
  public void testGetIndividualTypeArchive_otherUser() {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setDataType(singleton(DONOR));
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user("otherUser")
        .controlled(true)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    client()
        .resource(RESOURCE)
        .path(jobId)
        .path(DONOR.getId())
        .cookie(new Cookie(CrowdProperties.SESSION_TOKEN_NAME, sessionToken.toString()))
        .get(ClientResponse.class);
  }

  @Test
  public void testGetIndividualTypeArchive_missingDownloadType() {
    val jobId = "jobId1";
    val jobResponse = new JobResponse();
    jobResponse.setDataType(singleton(DONOR));
    jobResponse.setJobInfo(JobUiInfo.builder()
        .user(ANONYMOUS_USER)
        .controlled(false)
        .build());

    when(downloadClient.getJob(jobId)).thenReturn(jobResponse);

    val response = client()
        .resource(RESOURCE)
        .path(jobId)
        .path("fake")
        .get(ClientResponse.class);

    assertThat(response.getStatus()).isEqualTo(400);
  }
}
