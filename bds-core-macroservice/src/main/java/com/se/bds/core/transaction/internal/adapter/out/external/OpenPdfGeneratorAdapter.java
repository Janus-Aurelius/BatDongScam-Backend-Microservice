package com.se.bds.core.transaction.internal.adapter.out.external;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.se.bds.core.transaction.internal.application.port.out.PdfGeneratorPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Component
@Slf4j
public class OpenPdfGeneratorAdapter implements PdfGeneratorPort {

    @Override
    public byte[] generatePdf(String templateName, Map<String, Object> data) {
        log.info("[EVENT] Generating PDF using template: {}", templateName);
        
        // TODO: verify PDF template rendering with real contract data
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
            Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);

            document.add(new Paragraph("BATDONGSAN REAL ESTATE CONTRACT", titleFont));
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("This contract is generated electronically and secured by Escrow Protection.", bodyFont));
            document.add(new Paragraph("\nContract details:", boldFont));

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                document.add(new Paragraph(entry.getKey() + ": " + entry.getValue(), bodyFont));
            }

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Terms & Conditions:", boldFont));
            document.add(new Paragraph("1. The tenant/buyer agrees to hold security deposit in an authorized Escrow account.", bodyFont));
            document.add(new Paragraph("2. Payouts are triggered automatically upon contract completion or agreed forfeiture.", bodyFont));

            document.close();
            log.info("[EVENT] Contract PDF generated successfully, sizeBytes={}", out.size());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[EVENT] Contract PDF generation FAILED: template={}, error={}", templateName, e.getMessage());
            throw new RuntimeException("Failed to generate PDF contract", e);
        }
    }
}
