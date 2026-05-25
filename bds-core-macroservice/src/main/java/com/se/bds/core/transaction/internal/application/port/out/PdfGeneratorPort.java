package com.se.bds.core.transaction.internal.application.port.out;

import java.util.Map;

/**
 * Outbound port for PDF document generation (US-008).
 * Abstracts the PDF engine (OpenPDF) from the business logic.
 */
public interface PdfGeneratorPort {

    /**
     * Generates a PDF document from a template with provided data.
     *
     * @param templateName the name of the PDF template to use
     * @param data         key-value data to populate the template
     * @return the generated PDF as a byte array
     */
    byte[] generatePdf(String templateName, Map<String, Object> data);
}
