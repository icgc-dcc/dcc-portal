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
package org.icgc.dcc.portal.server.util;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import lombok.val;

/**
 * @see https://github.com/spring-projects/spring-boot/issues/3627#issuecomment-207404344
 * @see http://stackoverflow.com/questions/31011577/spring-boot-cannot-find-urlrewrite-xml-inside-jar-file
 */
public class TuckeyFilter extends UrlRewriteFilter {

  @Override
  protected void loadUrlRewriter(FilterConfig filterConfig) throws ServletException {
    // We have to load the url ourself, not with the servlet class loaded as tuckey would do it
    val confPath = "urlrewrite.xml";
    val confUrl = getClass().getClassLoader().getResource(confPath);
    val config = getClass().getClassLoader().getResourceAsStream(confPath);

    val conf = new Conf(filterConfig.getServletContext(), config, confPath, confUrl.toString(), false);
    checkConf(conf);
  }

}