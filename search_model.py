#!/usr/bin/env python3
"""Download TFLite face recognition models."""

import urllib.request
import ssl
import os

ssl._create_default_https_context = ssl._create_unverified_context

# MediaPipe official models - these should be accessible
models = [
    # MediaPipe Face Landmarker (can extract 468 face landmarks for embedding)
    ('https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task',
     'face_landmarker.task'),
    # MediaPipe Face Detector
    ('https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/1/blaze_face_short_range.tflite',
     'face_detector.tflite'),
    # MobileNet V2 as fallback feature extractor (from TF official)
    ('https://tfhub.dev/google/lite-model/imagenet/mobilenet_v2_100_224/feature_vector/2/default/1?lite-format=tflite',
     'mobilenet_v2_feature_vector.tflite'),
]

assets_dir = 'app/src/main/assets'
os.makedirs(assets_dir, exist_ok=True)

downloaded_any = False

for url, filename in models:
    output_path = os.path.join(assets_dir, filename)
    try:
        print(f'Downloading: {filename}')
        print(f'  URL: {url}')
        
        req = urllib.request.Request(url, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        })
        
        with urllib.request.urlopen(req, timeout=120) as response:
            content = response.read()
            with open(output_path, 'wb') as f:
                f.write(content)
        
        size = os.path.getsize(output_path)
        print(f'  Downloaded: {size} bytes')
        
        if size > 50000:  # > 50KB is likely valid
            print(f'  SUCCESS: {filename}')
            downloaded_any = True
        else:
            print(f'  WARNING: File may be invalid (too small)')
            os.remove(output_path)
            
    except Exception as e:
        print(f'  FAILED: {e}')
        continue

if downloaded_any:
    print('\n=== Download complete ===')
    print('Files downloaded to:', assets_dir)
    for f in os.listdir(assets_dir):
        fpath = os.path.join(assets_dir, f)
        if os.path.isfile(fpath):
            print(f'  - {f}: {os.path.getsize(fpath)} bytes')
    exit(0)
else:
    print('\nNo models could be downloaded')
    exit(1)
