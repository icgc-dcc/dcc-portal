/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.server.spring;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.repeat;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PRIVATE;

import java.net.UnknownHostException;
import java.sql.SQLException;

import org.elasticsearch.ElasticsearchException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

import lombok.NoArgsConstructor;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
class SpringBootFailureAnalyzers {

  static class UnsatisfiedDependencyFailureAnalyzer extends AbstractFailureAnalyzer<UnsatisfiedDependencyException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, UnsatisfiedDependencyException cause) {
      val lines = newLinkedHashSet(newArrayList(cause.getMessage().split("(?=Unsatisfied dependency)")));
      val message = lines.stream().map(l -> {
        String result = "";
        int i = 0;
        for (String s : l.split("(?<=:(?! \\{))")) {
          if (isNullOrEmpty(s)) continue;
          result += "\n" + repeat(" ", i++) + s;
        }

        return result;
      }).collect(joining("\n"));

      return new FailureAnalysis(
          "Portal failed to start due to a Spring bean error: \n\"" + message + "\n\"\n",
          "Review the stack trace and configuration",
          cause);
    }

  }

  static class UnknownHostFailureAnalyzer extends AbstractFailureAnalyzer<UnknownHostException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, UnknownHostException cause) {
      return new FailureAnalysis(
          "Portal failed to start due to a bad host configuration: \"" + cause.getMessage() + "\"",
          "Ensure the host is reachable on the current network and reconfigure application propertie(s)",
          cause);
    }

  }

  static class JdbcFailureAnalyzer extends AbstractFailureAnalyzer<SQLException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, SQLException cause) {
      return new FailureAnalysis(
          "Portal failed to start due to JDBC error: \"" + cause.getMessage() + "\"",
          "Ensure the database configuration is set correctly",
          cause);
    }

  }

  static class ElasticsearchFailureAnalyzer extends AbstractFailureAnalyzer<ElasticsearchException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ElasticsearchException cause) {
      return new FailureAnalysis(
          "Portal failed to start due to an Elasticsearch error: \"" + cause.getMessage() + "\"",
          "Ensure the ES host and indexes are configured correctly",
          cause);
    }

  }

}
