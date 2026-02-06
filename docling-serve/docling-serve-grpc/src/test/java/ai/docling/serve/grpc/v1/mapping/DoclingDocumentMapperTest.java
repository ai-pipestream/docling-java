package ai.docling.serve.grpc.v1.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.math.BigInteger;
import java.util.List;

import ai.docling.core.DoclingDocument;
import ai.docling.core.v1.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for DoclingDocumentMapper null-safety.
 * Every nullable field that we guard must have a regression test here
 * so removing a null check causes a test failure (NPE).
 */
class DoclingDocumentMapperTest {

    @Test
    void mapNullDocumentReturnsDefault() {
        var result = DoclingDocumentMapper.map(null);
        assertThat(result).isEqualTo(ai.docling.core.v1.DoclingDocument.getDefaultInstance());
    }

    @Nested
    class DocumentOriginNullSafety {

        @Test
        void handlesAllNullFields() {
            var origin = DoclingDocument.DocumentOrigin.builder().build();
            var doc = DoclingDocument.builder().name("test").origin(origin).build();

            assertThatNoException().isThrownBy(() -> DoclingDocumentMapper.map(doc));

            var proto = DoclingDocumentMapper.map(doc);
            assertThat(proto.hasOrigin()).isTrue();
            assertThat(proto.getOrigin().getMimetype()).isEmpty();
            assertThat(proto.getOrigin().getBinaryHash()).isEmpty();
            assertThat(proto.getOrigin().getFilename()).isEmpty();
        }

        @Test
        void mapsPopulatedFields() {
            var origin = DoclingDocument.DocumentOrigin.builder()
                .mimetype("application/pdf")
                .binaryHash(BigInteger.valueOf(12345))
                .filename("test.pdf")
                .uri("file:///test.pdf")
                .build();
            var doc = DoclingDocument.builder().name("test").origin(origin).build();

            var proto = DoclingDocumentMapper.map(doc);

            assertThat(proto.getOrigin().getMimetype()).isEqualTo("application/pdf");
            assertThat(proto.getOrigin().getBinaryHash()).isEqualTo("12345");
            assertThat(proto.getOrigin().getFilename()).isEqualTo("test.pdf");
            assertThat(proto.getOrigin().getUri()).isEqualTo("file:///test.pdf");
        }
    }

    @Nested
    class SectionHeaderNullSafety {

        @Test
        void handlesNullLevel() {
            var section = DoclingDocument.SectionHeaderItem.builder()
                .selfRef("#/texts/0")
                .label(DoclingDocument.DocItemLabel.SECTION_HEADER)
                .text("Header")
                .orig("Header")
                .contentLayer(DoclingDocument.ContentLayer.BODY)
                // level is null
                .build();
            var doc = DoclingDocument.builder().name("test").text(section).build();

            assertThatNoException().isThrownBy(() -> DoclingDocumentMapper.map(doc));

            var proto = DoclingDocumentMapper.map(doc);
            assertThat(proto.getTexts(0).getSectionHeader().getLevel()).isZero();
        }

        @Test
        void mapsPopulatedLevel() {
            var section = DoclingDocument.SectionHeaderItem.builder()
                .selfRef("#/texts/0")
                .label(DoclingDocument.DocItemLabel.SECTION_HEADER)
                .text("Header")
                .orig("Header")
                .contentLayer(DoclingDocument.ContentLayer.BODY)
                .level(3)
                .build();
            var doc = DoclingDocument.builder().name("test").text(section).build();

            var proto = DoclingDocumentMapper.map(doc);
            assertThat(proto.getTexts(0).getSectionHeader().getLevel()).isEqualTo(3);
        }
    }

    @Nested
    class FormattingNullSafety {

        @Test
        void handlesNullScript() {
            var formatting = DoclingDocument.Formatting.builder()
                .bold(true)
                .italic(false)
                .underline(false)
                .strikethrough(false)
                // script is null
                .build();
            var text = DoclingDocument.TextItem.builder()
                .selfRef("#/texts/0")
                .label(DoclingDocument.DocItemLabel.PARAGRAPH)
                .text("text").orig("text")
                .contentLayer(DoclingDocument.ContentLayer.BODY)
                .formatting(formatting)
                .build();
            var doc = DoclingDocument.builder().name("test").text(text).build();

            assertThatNoException().isThrownBy(() -> DoclingDocumentMapper.map(doc));

            var proto = DoclingDocumentMapper.map(doc);
            var protoFormatting = proto.getTexts(0).getText().getBase().getFormatting();
            assertThat(protoFormatting.getBold()).isTrue();
            assertThat(protoFormatting.getScript()).isEqualTo(Script.SCRIPT_UNSPECIFIED);
        }
    }

    @Nested
    class ProvenanceNullSafety {

        @Test
        void handlesNullBbox() {
            var prov = DoclingDocument.ProvenanceItem.builder()
                .pageNo(1)
                // bbox is null
                .build();

            assertThatNoException().isThrownBy(
                () -> DoclingDocumentMapper.mapProvenanceItem(prov));

            var proto = DoclingDocumentMapper.mapProvenanceItem(prov);
            assertThat(proto.getPageNo()).isEqualTo(1);
            assertThat(proto.hasBbox()).isFalse();
        }

        @Test
        void mapsPopulatedBbox() {
            var prov = DoclingDocument.ProvenanceItem.builder()
                .pageNo(1)
                .bbox(DoclingDocument.BoundingBox.builder()
                    .l(10.0).t(20.0).r(100.0).b(50.0).build())
                .charspan(List.of(0, 10))
                .build();

            var proto = DoclingDocumentMapper.mapProvenanceItem(prov);
            assertThat(proto.hasBbox()).isTrue();
            assertThat(proto.getBbox().getL()).isEqualTo(10.0);
            assertThat(proto.getCharspanCount()).isEqualTo(2);
        }
    }

    @Nested
    class BoundingBoxNullSafety {

        @Test
        void handlesAllNullCoordinates() {
            var bbox = DoclingDocument.BoundingBox.builder().build();

            assertThatNoException().isThrownBy(
                () -> DoclingDocumentMapper.mapBoundingBox(bbox));

            var proto = DoclingDocumentMapper.mapBoundingBox(bbox);
            assertThat(proto.getL()).isZero();
            assertThat(proto.getT()).isZero();
            assertThat(proto.getR()).isZero();
            assertThat(proto.getB()).isZero();
        }

        @Test
        void mapsPopulatedCoordinates() {
            var bbox = DoclingDocument.BoundingBox.builder()
                .l(1.0).t(2.0).r(3.0).b(4.0).build();

            var proto = DoclingDocumentMapper.mapBoundingBox(bbox);
            assertThat(proto.getL()).isEqualTo(1.0);
            assertThat(proto.getT()).isEqualTo(2.0);
            assertThat(proto.getR()).isEqualTo(3.0);
            assertThat(proto.getB()).isEqualTo(4.0);
        }
    }

    @Nested
    class TableDataNullSafety {

        @Test
        void handlesNullNumRowsAndNumCols() {
            var tableData = DoclingDocument.TableData.builder()
                // numRows, numCols null; grid defaults to empty
                .build();
            var table = DoclingDocument.TableItem.builder()
                .selfRef("#/tables/0")
                .contentLayer(DoclingDocument.ContentLayer.BODY)
                .data(tableData)
                .build();
            var doc = DoclingDocument.builder().name("test").table(table).build();

            assertThatNoException().isThrownBy(() -> DoclingDocumentMapper.map(doc));

            var proto = DoclingDocumentMapper.map(doc);
            assertThat(proto.getTables(0).getData().getNumRows()).isZero();
            assertThat(proto.getTables(0).getData().getNumCols()).isZero();
        }

        @Test
        void mapsPopulatedNumRowsAndNumCols() {
            var tableData = DoclingDocument.TableData.builder()
                .numRows(5).numCols(3).build();
            var table = DoclingDocument.TableItem.builder()
                .selfRef("#/tables/0")
                .contentLayer(DoclingDocument.ContentLayer.BODY)
                .data(tableData).build();
            var doc = DoclingDocument.builder().name("test").table(table).build();

            var proto = DoclingDocumentMapper.map(doc);
            assertThat(proto.getTables(0).getData().getNumRows()).isEqualTo(5);
            assertThat(proto.getTables(0).getData().getNumCols()).isEqualTo(3);
        }
    }

    @Nested
    class TableCellNullSafety {

        @Test
        void handlesAllNullFields() {
            // All boxed Integer fields + bbox + text are null
            var cell = DoclingDocument.TableCell.builder()
                .columnHeader(false).rowHeader(false)
                .rowSection(false).fillable(false)
                .build();
            var tableData = DoclingDocument.TableData.builder()
                .grid(List.of(List.of(cell))).build();
            var table = DoclingDocument.TableItem.builder()
                .selfRef("#/tables/0")
                .contentLayer(DoclingDocument.ContentLayer.BODY)
                .data(tableData).build();
            var doc = DoclingDocument.builder().name("test").table(table).build();

            assertThatNoException().isThrownBy(() -> DoclingDocumentMapper.map(doc));

            var proto = DoclingDocumentMapper.map(doc);
            var protoCell = proto.getTables(0).getData().getGrid(0).getCells(0);
            assertThat(protoCell.hasBbox()).isFalse();
            assertThat(protoCell.getRowSpan()).isZero();
            assertThat(protoCell.getColSpan()).isZero();
            assertThat(protoCell.getStartRowOffsetIdx()).isZero();
            assertThat(protoCell.getEndRowOffsetIdx()).isZero();
            assertThat(protoCell.getStartColOffsetIdx()).isZero();
            assertThat(protoCell.getEndColOffsetIdx()).isZero();
            assertThat(protoCell.getText()).isEmpty();
        }

        @Test
        void mapsPopulatedFields() {
            var cell = DoclingDocument.TableCell.builder()
                .bbox(DoclingDocument.BoundingBox.builder()
                    .l(1.0).t(2.0).r(3.0).b(4.0).build())
                .text("Cell")
                .rowSpan(2).colSpan(3)
                .startRowOffsetIdx(0).endRowOffsetIdx(2)
                .startColOffsetIdx(1).endColOffsetIdx(4)
                .columnHeader(true).rowHeader(false)
                .rowSection(false).fillable(false)
                .build();
            var tableData = DoclingDocument.TableData.builder()
                .grid(List.of(List.of(cell))).build();
            var table = DoclingDocument.TableItem.builder()
                .selfRef("#/tables/0")
                .contentLayer(DoclingDocument.ContentLayer.BODY)
                .data(tableData).build();
            var doc = DoclingDocument.builder().name("test").table(table).build();

            var proto = DoclingDocumentMapper.map(doc);
            var protoCell = proto.getTables(0).getData().getGrid(0).getCells(0);
            assertThat(protoCell.hasBbox()).isTrue();
            assertThat(protoCell.getBbox().getL()).isEqualTo(1.0);
            assertThat(protoCell.getText()).isEqualTo("Cell");
            assertThat(protoCell.getRowSpan()).isEqualTo(2);
            assertThat(protoCell.getColSpan()).isEqualTo(3);
            assertThat(protoCell.getStartRowOffsetIdx()).isZero();
            assertThat(protoCell.getEndRowOffsetIdx()).isEqualTo(2);
            assertThat(protoCell.getStartColOffsetIdx()).isEqualTo(1);
            assertThat(protoCell.getEndColOffsetIdx()).isEqualTo(4);
            assertThat(protoCell.getColumnHeader()).isTrue();
        }
    }

    @Nested
    class GraphCellNullSafety {

        @Test
        void handlesAllNullFields() {
            var cell = DoclingDocument.GraphCell.builder().build();
            var graph = DoclingDocument.GraphData.builder().cell(cell).build();
            var kv = DoclingDocument.KeyValueItem.builder()
                .selfRef("#/kv/0")
                .label("kv_region")
                .contentLayer(DoclingDocument.ContentLayer.BODY)
                .graph(graph)
                .build();
            var doc = DoclingDocument.builder().name("test").keyValueItem(kv).build();

            assertThatNoException().isThrownBy(() -> DoclingDocumentMapper.map(doc));

            var proto = DoclingDocumentMapper.map(doc);
            var protoCell = proto.getKeyValueItems(0).getGraph().getCells(0);
            assertThat(protoCell.getLabel()).isEqualTo(ai.docling.core.v1.GraphCellLabel.GRAPH_CELL_LABEL_UNSPECIFIED);
            assertThat(protoCell.getCellId()).isZero();
            assertThat(protoCell.getText()).isEmpty();
            assertThat(protoCell.getOrig()).isEmpty();
        }

        @Test
        void mapsPopulatedFields() {
            var cell = DoclingDocument.GraphCell.builder()
                .label(DoclingDocument.GraphCellLabel.KEY)
                .cellId(42)
                .text("Name:")
                .orig("Name:")
                .build();
            var graph = DoclingDocument.GraphData.builder().cell(cell).build();
            var kv = DoclingDocument.KeyValueItem.builder()
                .selfRef("#/kv/0").label("kv_region")
                .contentLayer(DoclingDocument.ContentLayer.BODY)
                .graph(graph).build();
            var doc = DoclingDocument.builder().name("test").keyValueItem(kv).build();

            var proto = DoclingDocumentMapper.map(doc);
            var protoCell = proto.getKeyValueItems(0).getGraph().getCells(0);
            assertThat(protoCell.getLabel()).isEqualTo(ai.docling.core.v1.GraphCellLabel.GRAPH_CELL_LABEL_KEY);
            assertThat(protoCell.getCellId()).isEqualTo(42);
            assertThat(protoCell.getText()).isEqualTo("Name:");
            assertThat(protoCell.getOrig()).isEqualTo("Name:");
        }
    }

    @Nested
    class GraphLinkNullSafety {

        @Test
        void handlesAllNullFields() {
            var link = DoclingDocument.GraphLink.builder().build();
            var graph = DoclingDocument.GraphData.builder().link(link).build();
            var kv = DoclingDocument.KeyValueItem.builder()
                .selfRef("#/kv/0").label("kv_region")
                .contentLayer(DoclingDocument.ContentLayer.BODY)
                .graph(graph).build();
            var doc = DoclingDocument.builder().name("test").keyValueItem(kv).build();

            assertThatNoException().isThrownBy(() -> DoclingDocumentMapper.map(doc));

            var proto = DoclingDocumentMapper.map(doc);
            var protoLink = proto.getKeyValueItems(0).getGraph().getLinks(0);
            assertThat(protoLink.getLabel()).isEqualTo(ai.docling.core.v1.GraphLinkLabel.GRAPH_LINK_LABEL_UNSPECIFIED);
            assertThat(protoLink.getSourceCellId()).isZero();
            assertThat(protoLink.getTargetCellId()).isZero();
        }

        @Test
        void mapsPopulatedFields() {
            var link = DoclingDocument.GraphLink.builder()
                .label(DoclingDocument.GraphLinkLabel.TO_VALUE)
                .sourceCellId(1).targetCellId(2).build();
            var graph = DoclingDocument.GraphData.builder().link(link).build();
            var kv = DoclingDocument.KeyValueItem.builder()
                .selfRef("#/kv/0").label("kv_region")
                .contentLayer(DoclingDocument.ContentLayer.BODY)
                .graph(graph).build();
            var doc = DoclingDocument.builder().name("test").keyValueItem(kv).build();

            var proto = DoclingDocumentMapper.map(doc);
            var protoLink = proto.getKeyValueItems(0).getGraph().getLinks(0);
            assertThat(protoLink.getLabel()).isEqualTo(ai.docling.core.v1.GraphLinkLabel.GRAPH_LINK_LABEL_TO_VALUE);
            assertThat(protoLink.getSourceCellId()).isEqualTo(1);
            assertThat(protoLink.getTargetCellId()).isEqualTo(2);
        }
    }

    @Nested
    class PageItemNullSafety {

        @Test
        void handlesNullSizeAndPageNo() {
            var page = DoclingDocument.PageItem.builder().build();
            var doc = DoclingDocument.builder().name("test").page("0", page).build();

            assertThatNoException().isThrownBy(() -> DoclingDocumentMapper.map(doc));

            var proto = DoclingDocumentMapper.map(doc);
            var protoPage = proto.getPagesMap().get("0");
            assertThat(protoPage.hasSize()).isFalse();
            assertThat(protoPage.getPageNo()).isZero();
        }

        @Test
        void mapsPopulatedFields() {
            var page = DoclingDocument.PageItem.builder()
                .pageNo(3)
                .size(DoclingDocument.Size.builder()
                    .width(800.0).height(600.0).build())
                .build();
            var doc = DoclingDocument.builder().name("test").page("3", page).build();

            var proto = DoclingDocumentMapper.map(doc);
            var protoPage = proto.getPagesMap().get("3");
            assertThat(protoPage.getPageNo()).isEqualTo(3);
            assertThat(protoPage.hasSize()).isTrue();
            assertThat(protoPage.getSize().getWidth()).isEqualTo(800.0);
            assertThat(protoPage.getSize().getHeight()).isEqualTo(600.0);
        }
    }

    @Nested
    class ImageRefNullSafety {

        @Test
        void handlesAllNullFields() {
            var imageRef = DoclingDocument.ImageRef.builder().build();
            var page = DoclingDocument.PageItem.builder()
                .pageNo(1).image(imageRef).build();
            var doc = DoclingDocument.builder().name("test").page("1", page).build();

            assertThatNoException().isThrownBy(() -> DoclingDocumentMapper.map(doc));

            var proto = DoclingDocumentMapper.map(doc);
            var protoPage = proto.getPagesMap().get("1");
            assertThat(protoPage.hasImage()).isTrue();
            var protoImage = protoPage.getImage();
            assertThat(protoImage.getMimetype()).isEmpty();
            assertThat(protoImage.getDpi()).isZero();
            assertThat(protoImage.hasSize()).isFalse();
            assertThat(protoImage.getUri()).isEmpty();
        }

        @Test
        void mapsPopulatedFields() {
            var imageRef = DoclingDocument.ImageRef.builder()
                .mimetype("image/png")
                .dpi(300)
                .size(DoclingDocument.Size.builder()
                    .width(800.0).height(600.0).build())
                .uri("file:///image.png")
                .build();
            var page = DoclingDocument.PageItem.builder()
                .pageNo(1).image(imageRef).build();
            var doc = DoclingDocument.builder().name("test").page("1", page).build();

            var proto = DoclingDocumentMapper.map(doc);
            var protoImage = proto.getPagesMap().get("1").getImage();
            assertThat(protoImage.getMimetype()).isEqualTo("image/png");
            assertThat(protoImage.getDpi()).isEqualTo(300);
            assertThat(protoImage.hasSize()).isTrue();
            assertThat(protoImage.getUri()).isEqualTo("file:///image.png");
        }
    }
}
