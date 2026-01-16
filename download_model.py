#!/usr/bin/env python3
"""Download MobileFaceNet TFLite model for face recognition."""

import urllib.request
import ssl
import os
import sys
import zipfile
import shutil
import tarfile

def download_model():
    # 禁用 SSL 验证
    ssl._create_default_https_context = ssl._create_unverified_context
    
    output_path = 'app/src/main/assets/mobilefacenet.tflite'
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    # 尝试多个下载源 - 使用 GitHub 上活跃的 Face Recognition 项目
    urls = [
        # Google Coral EdgeTPU models (official)
        'https://raw.githubusercontent.com/google-coral/test_data/master/mobilefacenet_edgetpu.tflite',
        'https://raw.githubusercontent.com/google-coral/test_data/master/facenet_keras_tflite.tflite',
        # FaceNet/MobileFaceNet implementations
        'https://github.com/ArnholdInstitute/Human_Face_Recognition/raw/main/models/facenet_keras.tflite',
        'https://github.com/nicholasyan/FaceNet-Tensorflow/raw/master/facenet.tflite',
        # Backup: use MobileNet as a fallback feature extractor
        'https://storage.googleapis.com/tfhub-lite-models/google/lite-model/imagenet/mobilenet_v2_100_224/feature_vector/2/default/1.tflite',
    ]
    
    for url in urls:
        try:
            print(f'Trying: {url}')
            temp_path = 'temp_model.tflite'
            
            # Create request with user agent
            req = urllib.request.Request(url, headers={
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
            })
            
            with urllib.request.urlopen(req, timeout=60) as response:
                with open(temp_path, 'wb') as out_file:
                    out_file.write(response.read())
            
            size = os.path.getsize(temp_path)
            print(f'Downloaded: {size} bytes')
            
            if size > 500000:  # > 500KB - likely a valid model
                shutil.move(temp_path, output_path)
                print(f'Success! Model saved to {output_path}')
                return True
            else:
                print('File too small, trying next source...')
                os.remove(temp_path)
        except Exception as e:
            print(f'Failed: {e}')
            if os.path.exists('temp_model.tflite'):
                os.remove('temp_model.tflite')
            continue
    
    return False

if __name__ == '__main__':
    success = download_model()
    sys.exit(0 if success else 1)
