package ai.docling.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.docling.core.DoclingDocument.BaseMeta;
import ai.docling.core.DoclingDocument.ContentLayer;
import ai.docling.core.DoclingDocument.DocItemLabel;
import ai.docling.core.DoclingDocument.EntitiesMetaField;
import ai.docling.core.DoclingDocument.EntityMention;
import ai.docling.core.DoclingDocument.FieldHeadingItem;
import ai.docling.core.DoclingDocument.FieldItem;
import ai.docling.core.DoclingDocument.FieldRegionItem;
import ai.docling.core.DoclingDocument.FieldValueItem;
import ai.docling.core.DoclingDocument.FineRef;
import ai.docling.core.DoclingDocument.GroupItem;
import ai.docling.core.DoclingDocument.GroupLabel;
import ai.docling.core.DoclingDocument.KeywordsMetaField;
import ai.docling.core.DoclingDocument.LanguageMetaField;
import ai.docling.core.DoclingDocument.TextItem;
import ai.docling.core.DoclingDocument.TitleItem;
import ai.docling.core.DoclingDocument.TopicsMetaField;
import ai.docling.core.DoclingDocument.TrackSource;

/**
 * Unit tests for {@link DoclingDocument}.
 */
class DoclingDocumentTests {

  @Test
  void shouldBuildEmptyDocument() {
    DoclingDocument document = DoclingDocument.builder().build();
    assertThat(document).isNotNull();
  }

  @Test
  void shouldBuildDocumentWithProperties() {
    DoclingDocument document = DoclingDocument.builder()
        .name("test-document")
        .text(TitleItem.builder()
            .label(DocItemLabel.TITLE)
            .text("Docling Rocks!")
            .build())
        .build();
    assertThat(document.getName()).isEqualTo("test-document");
    assertThat(document.getTexts().get(0)).isInstanceOf(TitleItem.class);

    TitleItem titleItem = (TitleItem) document.getTexts().get(0);
    assertThat(titleItem.getLabel()).isEqualTo(DocItemLabel.TITLE);
    assertThat(titleItem.getText()).isEqualTo("Docling Rocks!");
  }

  @Test
  void shouldSerializeFurnitureField() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    GroupItem furniture = GroupItem.builder()
        .selfRef("#/furniture")
        .contentLayer(ContentLayer.FURNITURE)
        .label(GroupLabel.UNSPECIFIED)
        .name("_root_")
        .build();

    DoclingDocument document = DoclingDocument.builder()
        .name("test-document")
        .furniture(furniture)
        .build();

    String json = mapper.writeValueAsString(document);

    assertThat(json).contains("\"furniture\"");
    assertThat(json).contains("\"content_layer\":\"furniture\"");
  }

  @Test
  void shouldDeserializeFurnitureField() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = """
        {
          "name": "test-document",
          "furniture": {
            "self_ref": "#/furniture",
            "children": [],
            "content_layer": "furniture",
            "name": "_root_",
            "label": "unspecified"
          }
        }
        """;

    DoclingDocument document = mapper.readValue(json, DoclingDocument.class);

    assertThat(document.getFurniture()).isNotNull();
    assertThat(document.getFurniture().getSelfRef()).isEqualTo("#/furniture");
    assertThat(document.getFurniture().getContentLayer()).isEqualTo(ContentLayer.FURNITURE);
    assertThat(document.getFurniture().getLabel()).isEqualTo(GroupLabel.UNSPECIFIED);
  }

  @Test
  void shouldDeserializeFineRefWithDollarRefKey() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = """
        {"$ref": "#/texts/0", "range": [2, 7]}
        """;

    FineRef ref = mapper.readValue(json, FineRef.class);

    assertThat(ref.getRef()).isEqualTo("#/texts/0");
    assertThat(ref.getRange()).containsExactly(2, 7);
  }

  @Test
  void shouldSerializeFineRefWithDollarRefKey() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    FineRef ref = FineRef.builder()
        .ref("#/texts/1")
        .range(0)
        .range(5)
        .build();

    String json = mapper.writeValueAsString(ref);

    assertThat(json).contains("\"$ref\":\"#/texts/1\"");
    assertThat(json).contains("\"range\":[0,5]");
  }

  @Test
  void shouldDeserializeEntityMentionCharspanAsArray() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = """
        {"text": "IBM", "label": "ORG", "charspan": [0, 3]}
        """;

    EntityMention mention = mapper.readValue(json, EntityMention.class);

    assertThat(mention.getText()).isEqualTo("IBM");
    assertThat(mention.getLabel()).isEqualTo("ORG");
    assertThat(mention.getCharspan()).containsExactly(0, 3);
  }

  @Test
  void shouldDeserializeBaseMetaWithAllFields() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = """
        {
          "summary": {"text": "A short summary.", "confidence": 0.9},
          "language": {"code": "en"},
          "entities": {"mentions": [{"text": "IBM", "label": "ORG", "charspan": [0, 3]}]},
          "keywords": {"values": ["transformer", "attention"]},
          "topics": {"values": ["machine learning"]}
        }
        """;

    BaseMeta meta = mapper.readValue(json, BaseMeta.class);

    assertThat(meta.getSummary()).isNotNull();
    assertThat(meta.getSummary().getText()).isEqualTo("A short summary.");
    assertThat(meta.getLanguage()).isNotNull();
    assertThat(meta.getLanguage().getCode()).isEqualTo("en");
    assertThat(meta.getEntities()).isNotNull();
    assertThat(meta.getEntities().getMentions()).hasSize(1);
    assertThat(meta.getEntities().getMentions().get(0).getText()).isEqualTo("IBM");
    assertThat(meta.getEntities().getMentions().get(0).getCharspan()).containsExactly(0, 3);
    assertThat(meta.getKeywords()).isNotNull();
    assertThat(meta.getKeywords().getValues()).containsExactly("transformer", "attention");
    assertThat(meta.getTopics()).isNotNull();
    assertThat(meta.getTopics().getValues()).containsExactly("machine learning");
  }

  @Test
  void shouldDeserializeTrackSourceAsFlatObject() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = """
        {"kind": "track", "start_time": 1.25, "end_time": 2.5, "voice": "John"}
        """;

    TrackSource source = mapper.readValue(json, TrackSource.class);

    assertThat(source.getKind()).isEqualTo("track");
    assertThat(source.getStartTime()).isEqualTo(1.25);
    assertThat(source.getEndTime()).isEqualTo(2.5);
    assertThat(source.getVoice()).isEqualTo("John");
  }

  @Test
  void shouldDeserializeTextItemWithSourceAndComments() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = """
        {
          "self_ref": "#/texts/0",
          "content_layer": "body",
          "label": "paragraph",
          "orig": "hello",
          "text": "hello",
          "source": [{"kind": "track", "start_time": 0.0, "end_time": 1.0}],
          "comments": [{"$ref": "#/texts/1", "range": [0, 5]}]
        }
        """;

    TextItem item = mapper.readValue(json, TextItem.class);

    assertThat(item.getSource()).hasSize(1);
    assertThat(item.getSource().get(0).getKind()).isEqualTo("track");
    assertThat(item.getComments()).hasSize(1);
    assertThat(item.getComments().get(0).getRef()).isEqualTo("#/texts/1");
    assertThat(item.getComments().get(0).getRange()).containsExactly(0, 5);
  }

  @Test
  void shouldDeserializeFieldHeadingItem() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = """
        {
          "self_ref": "#/texts/0",
          "content_layer": "body",
          "label": "field_heading",
          "orig": "Name",
          "text": "Name",
          "level": 1
        }
        """;

    FieldHeadingItem item = mapper.readValue(json, FieldHeadingItem.class);

    assertThat(item.getLabel()).isEqualTo(DocItemLabel.FIELD_HEADING);
    assertThat(item.getText()).isEqualTo("Name");
    assertThat(item.getLevel()).isEqualTo(1);
  }

  @Test
  void shouldDeserializeFieldValueItem() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = """
        {
          "self_ref": "#/texts/0",
          "content_layer": "body",
          "label": "field_value",
          "orig": "Alice",
          "text": "Alice",
          "kind": "fillable"
        }
        """;

    FieldValueItem item = mapper.readValue(json, FieldValueItem.class);

    assertThat(item.getLabel()).isEqualTo(DocItemLabel.FIELD_VALUE);
    assertThat(item.getText()).isEqualTo("Alice");
    assertThat(item.getKind()).isEqualTo("fillable");
  }

  @Test
  void shouldDeserializeFieldRegionAndFieldItemsInDocument() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = """
        {
          "name": "form-doc",
          "field_regions": [
            {
              "self_ref": "#/field_regions/0",
              "content_layer": "body",
              "label": "field_region",
              "prov": []
            }
          ],
          "field_items": [
            {
              "self_ref": "#/field_items/0",
              "content_layer": "body",
              "label": "field_item",
              "prov": []
            }
          ]
        }
        """;

    DoclingDocument document = mapper.readValue(json, DoclingDocument.class);

    assertThat(document.getFieldRegions()).hasSize(1);
    assertThat(document.getFieldRegions().get(0).getLabel()).isEqualTo(DocItemLabel.FIELD_REGION);
    assertThat(document.getFieldItems()).hasSize(1);
    assertThat(document.getFieldItems().get(0).getLabel()).isEqualTo(DocItemLabel.FIELD_ITEM);
  }

  @Test
  void shouldDeserializeFieldHeadingItemViaBaseTextItemPolymorphism() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = """
        {
          "name": "form-doc",
          "texts": [
            {
              "self_ref": "#/texts/0",
              "content_layer": "body",
              "label": "field_heading",
              "orig": "Section",
              "text": "Section",
              "level": 2
            },
            {
              "self_ref": "#/texts/1",
              "content_layer": "body",
              "label": "field_value",
              "orig": "42",
              "text": "42",
              "kind": "read_only"
            }
          ]
        }
        """;

    DoclingDocument document = mapper.readValue(json, DoclingDocument.class);

    assertThat(document.getTexts()).hasSize(2);
    assertThat(document.getTexts().get(0)).isInstanceOf(FieldHeadingItem.class);
    FieldHeadingItem heading = (FieldHeadingItem) document.getTexts().get(0);
    assertThat(heading.getLevel()).isEqualTo(2);

    assertThat(document.getTexts().get(1)).isInstanceOf(FieldValueItem.class);
    FieldValueItem value = (FieldValueItem) document.getTexts().get(1);
    assertThat(value.getKind()).isEqualTo("read_only");
  }

  @Test
  void shouldTreatNullFieldRegionsAndFieldItemsAsEmpty() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = """
        {
          "name": "doc",
          "field_regions": null,
          "field_items": null
        }
        """;

    DoclingDocument document = mapper.readValue(json, DoclingDocument.class);

    assertThat(document.getFieldRegions()).isEmpty();
    assertThat(document.getFieldItems()).isEmpty();
  }

}
