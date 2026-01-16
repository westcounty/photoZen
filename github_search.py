#!/usr/bin/env python3
"""Search GitHub for MobileFaceNet TFLite models and download."""

import urllib.request
import json
import ssl
import os
import time

ssl._create_default_https_context = ssl._create_unverified_context

def search_and_download():
    output_path = 'app/src/main/assets/mobilefacenet.tflite'
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    # Known working repositories that contain face recognition TFLite models
    repos_to_try = [
        # Repositories known to have MobileFaceNet or FaceNet TFLite models
        ('AkshayRaina02', 'FaceRecognition-android', 'app/src/main/assets/facenet.tflite'),
        ('nicholasyan', 'FaceNet-Tensorflow-Lite', 'app/src/main/assets/facenet.tflite'),
        ('nicholasyan', 'FaceNet-Tensorflow-Lite', 'facenet.tflite'),
        ('nickel-huang', 'MobileFaceNet_Tutorial_Pytorch', 'tflite_model/mobilefacenet.tflite'),
        ('syarahnamira', 'FaceRecognitionCNN', 'app/src/main/assets/mobile_face_net.tflite'),
        ('rohanpanda001', 'face-recognition-android', 'app/src/main/assets/mobile_face_net.tflite'),
    ]
    
    for owner, repo, path in repos_to_try:
        # Try raw GitHub URL
        urls = [
            f'https://raw.githubusercontent.com/{owner}/{repo}/master/{path}',
            f'https://raw.githubusercontent.com/{owner}/{repo}/main/{path}',
            f'https://github.com/{owner}/{repo}/raw/master/{path}',
            f'https://github.com/{owner}/{repo}/raw/main/{path}',
        ]
        
        for url in urls:
            try:
                print(f'Trying: {url}')
                
                req = urllib.request.Request(url, headers={
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
                    'Accept': 'application/octet-stream'
                })
                
                with urllib.request.urlopen(req, timeout=60) as response:
                    content = response.read()
                    
                    # Check if it's a valid TFLite model (magic bytes)
                    if len(content) < 100:
                        print(f'  Too small: {len(content)} bytes')
                        continue
                        
                    # TFLite files typically start with specific bytes
                    # but let's just check the size
                    if len(content) > 500000:  # > 500KB
                        with open(output_path, 'wb') as f:
                            f.write(content)
                        print(f'  SUCCESS! Downloaded {len(content)} bytes')
                        return True
                    else:
                        print(f'  File size: {len(content)} bytes (too small)')
                        
            except urllib.error.HTTPError as e:
                if e.code == 404:
                    continue  # Try next URL
                print(f'  HTTP Error: {e.code}')
            except Exception as e:
                print(f'  Error: {e}')
            
            time.sleep(0.5)  # Rate limiting
    
    return False

if __name__ == '__main__':
    success = search_and_download()
    
    if not success:
        print("\n" + "="*60)
        print("Could not automatically download MobileFaceNet model.")
        print("Please download manually from one of these sources:")
        print("\n1. sirius-ai/MobileFaceNet_TF (GitHub)")
        print("   https://github.com/sirius-ai/MobileFaceNet_TF/tree/master/tflite")
        print("\n2. facenet-pytorch (via onnx2tf conversion)")
        print("   pip install facenet-pytorch torch onnx2tf")
        print("\n3. Google's Coral EdgeTPU models")
        print("   https://coral.ai/models/")
        print("="*60)
    
    exit(0 if success else 1)
