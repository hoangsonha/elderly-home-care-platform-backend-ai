package com.capstone_project.elderly_platform.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class QRCodeGeneration {

  private static final int QR_CODE_WIDTH = 500; // Tăng kích thước để dễ quét hơn
  private static final int QR_CODE_HEIGHT = 500;
  private static final int QUIET_ZONE = 4; // Margin xung quanh QR code

  /**
   * Generate QR code image from URL and return as Base64 string
   *
   * @param url The URL to encode in QR code
   * @return Base64 encoded PNG image string (without data:image/png;base64,
   *         prefix)
   */
  public String generateQRCodeBase64(String url) throws WriterException, IOException {
    // Validate URL
    if (url == null || url.trim().isEmpty()) {
      throw new IllegalArgumentException("URL cannot be null or empty");
    }

    // Tạo hints để cải thiện chất lượng QR code
    Map<EncodeHintType, Object> hints = new HashMap<>();
    hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // Error correction cao nhất
    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    hints.put(EncodeHintType.MARGIN, QUIET_ZONE); // Thêm margin

    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT, hints);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

    return Base64.getEncoder().encodeToString(outputStream.toByteArray());
  }
}

