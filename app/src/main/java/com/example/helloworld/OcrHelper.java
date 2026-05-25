package com.example.helloworld;

import android.graphics.Bitmap;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.android.gms.tasks.Tasks;

/**
 * ML Kit OCR 文字识别工具类
 * 中文识别器 + 图片预处理
 */
public class OcrHelper {

    private static final TextRecognizer recognizer =
            TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());

    /**
     * 识别图片中的文字（预处理+识别）
     */
    public static String recognizeText(Bitmap bitmap) {
        try {
            // 1. 图片预处理（灰度+对比度增强）
            Bitmap processed = OcrPreprocessor.preprocess(bitmap);
            if (processed == null) return "";

            // 2. 识别
            InputImage image = InputImage.fromBitmap(processed, 0);
            String result = Tasks.await(recognizer.process(image)).getText();

            // 3. 释放预处理图片
            if (processed != bitmap && !processed.isRecycled()) {
                processed.recycle();
            }

            return result != null ? result : "";
        } catch (Exception e) {
            e.printStackTrace();
            // 降级：直接用原始图片
            try {
                InputImage image = InputImage.fromBitmap(bitmap, 0);
                return Tasks.await(recognizer.process(image)).getText();
            } catch (Exception e2) {
                return "";
            }
        }
    }
}
