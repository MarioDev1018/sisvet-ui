package com.donkan.sisvetclinic.modules.finance.infrastructure.output.document;

import com.donkan.sisvetclinic.modules.finance.service.dto.PdfReportDocument;
import com.donkan.sisvetclinic.modules.finance.service.dto.PdfReportSection;
import com.donkan.sisvetclinic.modules.finance.service.port.out.ReportExportPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.springframework.stereotype.Component;

@Component
public class PdfReportAdapter implements ReportExportPort {

    private static final float LEFT = 45;
    private static final float RIGHT = 45;
    private static final float BOTTOM = 55;
    private static final float LINE_HEIGHT = 15;

    private static final PDType1Font REGULAR =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private static final PDType1Font BOLD =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    @Override
    public byte[] generatePdf(PdfReportDocument report) {

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(document);

            writer.writeHeading("SISVET-CLINIC", 16);
            writer.writeHeading(report.title(), 13);
            writer.writeText(report.subtitle(), 10);
            writer.blank();

            for (PdfReportSection section : report.sections()) {

                writer.ensureSpace(48);
                writer.writeHeading(section.heading(), 11);

                for (String line : section.lines()) {
                    writer.writeText(line, 9);
                }

                writer.blank();
            }

            writer.close();

            document.save(output);

            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el archivo PDF.", exception);
        }
    }

    private static class PdfWriter {

        private final PDDocument document;
        private PDPageContentStream stream;
        private float y;

        PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        void newPage() throws IOException {

            if (stream != null) {
                stream.close();
            }

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - 45;
        }

        void ensureSpace(float height) throws IOException {

            if (y - height < BOTTOM) {
                newPage();
            }
        }

        void writeHeading(String value, int size) throws IOException {
            writeParagraph(value, BOLD, size);
            y -= 5;
        }

        void writeText(String value, int size) throws IOException {
            writeParagraph(value, REGULAR, size);
        }

        void blank() {
            y -= 12;
        }

        void writeParagraph(String value, PDType1Font font, int size) throws IOException {

            float width = PDRectangle.A4.getWidth() - LEFT - RIGHT;

            for (String line : wrap(sanitize(value), font, size, width)) {

                ensureSpace(LINE_HEIGHT);

                stream.beginText();
                stream.setFont(font, size);
                stream.newLineAtOffset(LEFT, y);
                stream.showText(line);
                stream.endText();

                y -= LINE_HEIGHT;
            }
        }

        List<String> wrap(String value, PDType1Font font, int size, float maxWidth) throws IOException {

            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();

            for (String word : value.split("\\s+")) {

                String candidate = current.isEmpty() ? word : current + " " + word;

                if (font.getStringWidth(candidate) * size / 1000f <= maxWidth) {
                    current = new StringBuilder(candidate);
                    continue;
                }

                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }

                for (char character : word.toCharArray()) {

                    String next = current.toString() + character;

                    if (!current.isEmpty() && font.getStringWidth(next) * size / 1000f > maxWidth) {
                        lines.add(current.toString());
                        current.setLength(0);
                    }

                    current.append(character);
                }
            }

            if (!current.isEmpty()) {
                lines.add(current.toString());
            }

            return lines.isEmpty() ? List.of(" ") : lines;
        }

        String sanitize(String value) {

            if (value == null) {
                return "-";
            }

            return value
                    .replace('\n', ' ')
                    .replace('\r', ' ')
                    .replace('\t', ' ')
                    .replace('–', '-')
                    .replace('—', '-')
                    .replace('“', '"')
                    .replace('”', '"')
                    .replaceAll("[^\\x20-\\x7E\\u00A0-\\u00FF]", "?");
        }

        void close() throws IOException {

            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }
}