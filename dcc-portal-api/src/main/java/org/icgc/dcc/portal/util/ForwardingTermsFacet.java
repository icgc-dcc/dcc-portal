package org.icgc.dcc.portal.util;
import org.elasticsearch.search.facet.terms.TermsFacet;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public class ForwardingTermsFacet implements TermsFacet {

  @Delegate
  final TermsFacet delegate;

}