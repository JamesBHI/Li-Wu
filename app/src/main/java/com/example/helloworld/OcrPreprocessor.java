package com.example.helloworld;

import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Canvas;

/**
 * OCR 图片预处理 - 提高识别准确率
 */
public class OcrPreprocessor {

    /**
     * 预处理图片：灰度化 + 对比度增强 + 锐化
     * @param original 原始位图
     * @return 处理后的位图
     */
    public static Bitmap preprocess(Bitmap original) {
        if (original == null) return null;

        int width = original.getWidth();
        int height = original.getHeight();

        // 1. 创建可变的副本
        Bitmap processed = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(processed);

        // 2. 灰度化 + 对比度增强
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix(new float[]{
            1.5f, 0,    0,    0, -40,   // 红色通道增强对比度
            0,    1.5f, 0,    0, -40,   // 绿色通道
            0,    0,    1.5f, 0, -40,   // 蓝色通道
            0,    0,    0,    1, 0      // Alpha不变
        });
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(original, 0, 0, paint);

        // 3. 如果是彩色图，额外做一次灰度加强
        int[] pixels = new int[width * height];
        processed.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            // 自适应二值化阈值 - 使用亮度平均值
            int gray = (int)(0.299 * r + 0.587 * g + 0.114 * b);
            
            // 自适应阈值增强：让文字更清晰
            if (gray < 128) {
                gray = Math.max(0, gray - 30);
            } else {
                gray = Math.min(255, gray + 30);
            }

            pixels[i] = (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
        }

        processed.setPixels(pixels, 0, width, 0, 0, width, height);

        return processed;
    }

    /**
     * 获取图片的亮度直方图，用于判断是否需要预处理
     */
    public static boolean needsPreprocessing(Bitmap bitmap) {
        if (bitmap == null) return false;
        
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int totalPixels = width * height;
        
        // 采样判断：如果平均亮度太极端，需要预处理
        int sampleSize = Math.max(1, totalPixels / 1000);
        int brightSum = 0;
        int darkSum = 0;
        int sampleCount = 0;
        
        for (int y = 0; y < height; y += Math.max(1, height / 30)) {
            for (int x = 0; x < width; x += Math.max(1, width / 30)) {
                int pixel = bitmap.getPixel(x, y);
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                int gray = (r + g + b) / 3;
                
                if (gray > 200) brightSum++;
                else if (gray < 50) darkSum++;
                sampleCount++;
            }
        }
        
        // 如果太亮或太暗，需要预处理
        float brightRatio = (float) brightSum / sampleCount;
        float darkRatio = (float) darkSum / sampleCount;
        
        return brightRatio > 0.6f || darkRatio > 0.6f;
    }
}
