package com.chessdigitizer.backend.infrastructure.adapter.out;

import com.chessdigitizer.backend.application.config.GlobalProperties.StorageProperties;
import com.chessdigitizer.backend.domain.port.out.CurrentUserPort;
import com.chessdigitizer.backend.domain.port.out.PdfRenderer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;


@Component
public class LocalPdfRenderer implements PdfRenderer {

    private final StorageProperties storageProperties;
    private final CurrentUserPort currentUserPort;

    public LocalPdfRenderer(StorageProperties storageProperties, CurrentUserPort currentUserPort) {
        this.storageProperties = storageProperties;
        this.currentUserPort = currentUserPort;
    }

    @Override
    public byte[] renderPage(UUID bookId, int pageNumber, int dpi) {

        UUID ownerId = currentUserPort.getCurrentUserId();
        Path path = Paths.get(this.storageProperties.getBooksPath(), ownerId.toString(), bookId.toString() + ".pdf");
        if(!Files.exists(path)) {throw new RuntimeException("Book not found");}
        try(PDDocument doc = Loader.loadPDF(path.toFile())) {
            PDFRenderer pdfBoxRenderer = new PDFRenderer(doc);
            BufferedImage image = pdfBoxRenderer.renderImageWithDPI(pageNumber - 1 , dpi);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}