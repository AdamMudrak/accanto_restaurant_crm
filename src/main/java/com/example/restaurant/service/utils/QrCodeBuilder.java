package com.example.restaurant.service.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class QrCodeBuilder {

    private static final int QR_SIZE = 500;

    public static byte[] generatePng(String content) {
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(),
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN, 2
        );

        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    QR_SIZE,
                    QR_SIZE,
                    hints
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);

            return outputStream.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException(
                    "Failed to generate QR code",
                    exception
            );
        }
    }
}
