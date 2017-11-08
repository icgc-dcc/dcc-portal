package org.icgc.dcc.portal.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.val;
import org.dcc.portal.pql.ast.StatementNode;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PaginationRequest {
  @ApiModelProperty(value = "Number of records to return in this batch", required = true)
  Integer size;
  @ApiModelProperty(value = "The index of the first record in this batch", required = true)
  Integer from;
  @ApiModelProperty(value = "Name of the column to sort this record set", required = true)
  String sort;
  @ApiModelProperty(value = "Sorting order (e.g. asc or desc)", required = true)
  String order;

  public static PaginationRequest of(StatementNode pqlStatement) {
    Integer from = 1;
    Integer size = 20000;
    String sort = "";
    String order = "";

    if (pqlStatement.hasLimit()) {
      val limit = pqlStatement.getLimit();
      from = limit.getFrom();
      size = limit.getSize();
    }

    if (pqlStatement.hasSort()) {
      val fields = pqlStatement.getSort().getFields();
      val names = fields.keySet().asList();
      if (!names.isEmpty()) {
        sort = names.get(0);
        order = fields.get(sort).getSign().equals("+") ? "asc" : "desc";
      }
    }

    return new PaginationRequest(size, from, sort, order);
  }

  public static PaginationRequest of(Query query) {
    return new PaginationRequest(query.getSize(), query.getFrom(), query.getSort(), query.getOrder().toString());
  }

  protected PaginationRequest(PaginationRequest request) {
    this(request.size, request.from, request.sort, request.order);
  }

  protected PaginationRequest(Integer size, Integer from, String sort, String order) {
    this.size = size;
    this.from = from + 1;
    this.sort = sort;
    this.order = order.toLowerCase();
  }
}
