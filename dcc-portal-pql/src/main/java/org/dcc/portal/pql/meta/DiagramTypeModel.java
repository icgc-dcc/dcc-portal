package org.dcc.portal.pql.meta;

import static org.dcc.portal.pql.meta.field.StringFieldModel.string;

import java.util.List;
import java.util.Map;

import org.dcc.portal.pql.meta.field.FieldModel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.experimental.UtilityClass;

public class DiagramTypeModel extends TypeModel {

  public DiagramTypeModel() {
    super(Fields.MAPPINGS, INTERNAL_ALIASES, PUBLIC_FIELDS, INCLUDE_FIELDS);
  }

  @Override
  public Type getType() {
    return Type.DIAGRAM;
  }

  @Override
  public List<String> getFacets() {
    return null;
  }

  @Override
  public String prefix() {
    return Type.DIAGRAM.getPrefix();
  }

  /**
   * Field aliases
   */
  @UtilityClass
  public class Fields {

    public static String PATHWAY_ID = "pathwayId";
    public static String HIGHLIGHTS = "highlights";
    public static String XML = "xml";
    public static String PROTEIN_MAP = "protein_map";

    // Main mapping
    private final List<FieldModel> MAPPINGS = ImmutableList.<FieldModel> builder()
        .add(string(EsFields.DIAGRAM_ID, PATHWAY_ID)).build();
  }

  /**
   * Raw ES fields
   */
  @UtilityClass
  public class EsFields {

    public final String DIAGRAM_ID = "diagram_id";
    public final String XML = "xml";
    public final String HIGHLIGHTS = "highlights";
    public final String PROTEIN_MAP = "protein_map";

  }

  public static final List<String> PUBLIC_FIELDS = ImmutableList.of(Fields.PATHWAY_ID);

  private static final List<String> INCLUDE_FIELDS = ImmutableList.of(
      EsFields.DIAGRAM_ID,
      EsFields.XML,
      EsFields.HIGHLIGHTS,
      EsFields.PROTEIN_MAP);

  private static final Map<String, String> INTERNAL_ALIASES =
      ImmutableMap.<String, String> of(Fields.PATHWAY_ID, EsFields.DIAGRAM_ID);

}
