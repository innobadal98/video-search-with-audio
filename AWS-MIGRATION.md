# AWS Migration Guide - Service Mapping

## Current Local Setup → AWS Services

### Storage & File Management
- **Local uploads/ folders** → **Amazon S3**
- **Local temp/ folders** → **Amazon S3** (with lifecycle policies)

### Video Processing
- **Local FFmpeg** → **AWS Elemental MediaConvert**
- **Local OpenCV scene detection** → **Amazon Rekognition Video**
- **Local video analysis** → **Amazon Rekognition Video**

### Audio Processing
- **Local Whisper model** → **Amazon Transcribe**
- **Local audio extraction** → **AWS Elemental MediaConvert**

### AI/ML Models
- **Local SentenceTransformers (CLIP)** → **Amazon Bedrock (Titan Multimodal)**
- **Local text embeddings** → **Amazon Bedrock (Titan Text)**
- **Local model hosting** → **Amazon SageMaker**

### Database
- **MongoDB Atlas** → **Amazon DocumentDB** or **Amazon DynamoDB**
- **Vector storage** → **Amazon OpenSearch** (with vector search)

### Application Hosting
- **Local Spring Boot** → **AWS Elastic Beanstalk** or **Amazon ECS**
- **Local FastAPI** → **AWS Lambda** or **Amazon ECS**


### Security & Access
- **Local file access** → **AWS IAM roles**


### Monitoring & Logging
- **Local logs** → **Amazon CloudWatch**


### Cost Optimization Services
- **Always-on local services** → **AWS Lambda** (pay-per-use)
- **Fixed storage** → **S3 Intelligent Tiering**

## Migration Priority
1. **Storage** (S3) - Easiest migration
2. **Database** (DocumentDB/DynamoDB) - Critical data
3. **AI Services** (Bedrock/Rekognition) - Core functionality
4. **Application Hosting** (ECS/Lambda) - Infrastructure
5. **Monitoring** (CloudWatch) - Operations
