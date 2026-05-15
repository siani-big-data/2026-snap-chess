FROM python:3.11-slim

RUN apt-get update && apt-get install -y \
    libxcb1 libgl1 libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .

RUN pip install --no-cache-dir torch==2.2.2 torchvision==0.17.2 --index-url https://download.pytorch.org/whl/cpu
RUN pip install --no-cache-dir ultralytics==8.0.196
RUN pip install --no-cache-dir -r requirements.txt

COPY models/ models/
COPY app/ app/

EXPOSE 8003
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8003"]