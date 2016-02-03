/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.filter;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.hash.Hashing.murmur3_128;
import static javax.ws.rs.core.Response.fromResponse;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response.ResponseBuilder;

import lombok.val;

import org.elasticsearch.client.Client;
import org.icgc.dcc.portal.config.PortalProperties.CacheProperties;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashFunction;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

@Component
public class CachingFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private static final String DATE_FIELD_NAME = "date";
  private static final String RELEASE_TYPE_NAME = "release";
  private static final HashFunction HASH_FUNCTION = murmur3_128();

  private final boolean enableLastModified;
  private final boolean enableEtag;
  private final Date lastModifiedDate;

  // Resources for which ETag and LastModified should not be generated
  private final List<Pattern> excludeLastModifiedPatterns;
  private final List<Pattern> excludeEtagPatterns;

  @Autowired
  public CachingFilter(Client client, @Value("#{indexName}") String indexName, CacheProperties cacheConfig) {
    this.lastModifiedDate = getLastModified(client, indexName);
    this.enableLastModified = cacheConfig.isEnableLastModified();
    this.enableEtag = cacheConfig.isEnableETag();
    this.excludeLastModifiedPatterns = compilePatterns(cacheConfig.getExcludeLastModified());
    this.excludeEtagPatterns = compilePatterns(cacheConfig.getExcludeETag());
  }

  @Override
  public ContainerRequest filter(ContainerRequest request) {
    if (isLastModifiedPreconditioned(request) && !isEtagPreconditioned(request)) {
      val builder = request.evaluatePreconditions(lastModifiedDate);
      checkNotModified(builder);
    }

    return request;
  }

  @Override
  public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
    ResponseBuilder builder = null;
    val generateLastModified = isLastModifiedPreconditioned(request);

    if (isEtagPreconditioned(request)) {
      val etag = getEtag(response);
      val gzipEtag = getGzipEtag(etag);
      val hasEtag = hasEtag(etag);

      if (hasEtag && generateLastModified) {
        // Compare using "gzip" version
        builder = request.evaluatePreconditions(lastModifiedDate, gzipEtag);
      } else if (hasEtag) {
        builder = request.evaluatePreconditions(gzipEtag);
      } else if (generateLastModified) {
        builder = request.evaluatePreconditions(lastModifiedDate);
      }

      checkNotModified(builder);
      builder = fromResponse(response.getResponse());

      if (hasEtag(etag)) {
        builder.tag(etag);
      }

      if (generateLastModified) {
        builder.lastModified(lastModifiedDate);
      }

      response.setResponse(builder.build());
      // Both ETag and LastModified are enabled, but ETag could not be generated because of empty entity
    } else if (generateLastModified) {
      builder = fromResponse(response.getResponse());
      builder.lastModified(lastModifiedDate);
      response.setResponse(builder.build());
    }

    return response;
  }

  private static boolean hasEtag(EntityTag etag) {
    return etag != null;
  }

  private static void checkNotModified(ResponseBuilder builder) {
    // Do not process the request. Go straight to the response filters.
    val fresh = builder != null;
    if (fresh) {
      throw new WebApplicationException(builder.build());
    }
  }

  private static EntityTag getEtag(ContainerResponse response) {
    val entity = response.getEntity();
    if (hasEntity(entity)) {
      // Compute <hash> and <hash>-gzip eTags
      return entityTag(entity);
    }

    return null;
  }

  private static EntityTag getGzipEtag(EntityTag eTag) {
    return new EntityTag(eTag.getValue() + "-gzip");
  }

  private boolean isLastModifiedPreconditioned(ContainerRequest request) {
    return enableLastModified && isCacheable(request, excludeLastModifiedPatterns);
  }

  private boolean isEtagPreconditioned(ContainerRequest request) {
    return enableEtag && isCacheable(request, excludeEtagPatterns);
  }

  private static boolean hasEntity(Object entity) {
    return entity != null;
  }

  private static EntityTag entityTag(Object entity) {
    val text = entity.toString();
    val hash = HASH_FUNCTION.hashUnencodedChars(text).toString();

    return new EntityTag(hash);
  }

  private static Date getLastModified(Client client, String indexName) {
    val response = client.prepareSearch(indexName)
        .setTypes(RELEASE_TYPE_NAME)
        .addField(DATE_FIELD_NAME)
        .setSize(1)
        .execute()
        .actionGet();
    val hits = response.getHits().getHits();
    checkState(hits.length != 0, "Missing date of release for Last-Modified header");
    String value = hits[0].field(DATE_FIELD_NAME).getValue();
    checkNotNull(value, "Missing date of release for Last-Modified header");

    return parseIndexDate(value);
  }

  private static Date parseIndexDate(String value) {
    // Format used by the summarizer to create the release date
    return new Date(DateTime.parse(value).getMillis());
  }

  private boolean isCacheable(ContainerRequest request, List<Pattern> excludePatterns) {
    val path = request.getPath();
    for (val excludePattern : excludePatterns) {
      if (excludePattern.matcher(path).matches()) {
        return false;
      }
    }

    return true;
  }

  private static List<Pattern> compilePatterns(List<String> source) {
    val result = new ImmutableList.Builder<Pattern>();
    for (val regex : source) {
      result.add(Pattern.compile(regex));
    }

    return result.build();
  }

}
