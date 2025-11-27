FROM python:3.10-bullseye

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    git \
    wget \
    unzip \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    libgomp1 \
    libgl1 \
    libxrender1 \
    && rm -rf /var/lib/apt/lists/*

# Upgrade pip
RUN pip install --no-cache-dir --upgrade pip setuptools wheel

# Install numpy FIRST with strict upper bound
RUN pip install --no-cache-dir "numpy>=1.24.0,<1.25.0"

# Install packages with --no-deps first, then install their dependencies manually
RUN pip install --no-cache-dir \
    opencv-python-headless==4.8.1.78

# Install onnxruntime with constraints
RUN pip install --no-cache-dir \
    protobuf==3.20.3 \
    flatbuffers==23.5.26 \
    coloredlogs \
    sympy \
    packaging \
    onnxruntime==1.16.3

# Install insightface with --no-deps and then its dependencies
RUN pip install --no-cache-dir \
    Cython \
    matplotlib \
    scikit-learn \
    scikit-image \
    easydict \
    prettytable \
    albumentations

RUN pip install --no-cache-dir insightface==0.7.3

# Install web framework packages
RUN pip install --no-cache-dir \
    fastapi==0.104.1 \
    uvicorn==0.24.0 \
    python-multipart==0.0.6 \
    pillow==10.1.0

# CRITICAL: Force numpy back to 1.24 in case something upgraded it
RUN pip install --force-reinstall --no-cache-dir "numpy>=1.24.0,<1.25.0"

# Verify versions
RUN python -c "import numpy; print('NumPy:', numpy.__version__); assert numpy.__version__.startswith('1.24'), f'Wrong numpy version: {numpy.__version__}'" && \
    python -c "import cv2; print('OpenCV:', cv2.__version__)" && \
    python -c "import onnxruntime; print('ONNX Runtime:', onnxruntime.__version__)" && \
    python -c "import insightface; print('InsightFace:', insightface.__version__)"

# Show all installed packages to debug
RUN pip list | grep -i numpy

# Download AntelopeV2 model at build time
RUN mkdir -p /root/.insightface/models && \
    wget -O /root/.insightface/models/antelopev2.zip https://github.com/deepinsight/insightface/releases/download/v0.7/antelopev2.zip && \
    unzip /root/.insightface/models/antelopev2.zip -d /root/.insightface/models/ && \
    rm /root/.insightface/models/antelopev2.zip

# Copy application
COPY face_service.py .

# Set environment variable to prevent numpy upgrades
ENV NUMPY_EXPERIMENTAL_ARRAY_FUNCTION=0

# Expose port
EXPOSE 8000

# Run the application
CMD ["uvicorn", "face_service:app", "--host", "0.0.0.0", "--port", "8000"]
