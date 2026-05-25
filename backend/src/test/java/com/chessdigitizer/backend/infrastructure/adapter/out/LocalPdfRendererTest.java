package com.chessdigitizer.backend.infrastructure.adapter.out;

import com.chessdigitizer.backend.application.config.GlobalProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LocalPdfRendererTest {

    /** PDF mínimo de una página (mismo patrón que las pruebas de integración de libros). */
    private static final byte[] MINIMAL_PDF = """
            %PDF-1.0
            1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj 3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]>>endobj
            trailer<</Root 1 0 R/Size 4>>
            %%EOF""".getBytes();

    @Test
    void renderPage_returnsPngBytes(@TempDir Path tempDir) throws Exception {
        UUID bookId = UUID.randomUUID();
        Path pdfPath = tempDir.resolve(bookId + ".pdf");
        Files.write(pdfPath, MINIMAL_PDF);

        GlobalProperties.StorageProperties storage = new GlobalProperties.StorageProperties();
        storage.setBooksPath(tempDir.toString());
        storage.setChessPath(tempDir.resolve("chess").toString());
        Files.createDirectories(tempDir.resolve("chess"));

        LocalPdfRenderer renderer = new LocalPdfRenderer(storage);

        byte[] png = renderer.renderPage(bookId, 1, 96);

        assertNotNull(png);
        assertTrue(png.length > 50, "PNG debería tener cabecera y cuerpo");
        assertEquals((byte) 0x89, png[0]);
    }

    @Test
    void renderPage_throwsWhenPdfMissing(@TempDir Path tempDir) {
        GlobalProperties.StorageProperties storage = new GlobalProperties.StorageProperties();
        storage.setBooksPath(tempDir.toString());
        LocalPdfRenderer renderer = new LocalPdfRenderer(storage);

        assertThrows(RuntimeException.class, () -> renderer.renderPage(UUID.randomUUID(), 1, 72));
    }
}
