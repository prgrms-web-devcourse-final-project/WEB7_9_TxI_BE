terraform {
  // aws 라이브러리 불러옴
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

# AWS 설정 시작
provider "aws" {
  region = var.region
  default_tags {
    tags = {
      Project   = var.project_name
      ManagedBy = "Terraform"
      Team      = var.team_tag
    }
  }
}
# AWS 설정 끝


# Locals (네이밍 규칙)
locals {
  name = var.project_name
}

# VPC 설정 시작
resource "aws_vpc" "vpc_1" {
  cidr_block = "10.0.0.0/16"

  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${local.name}-vpc"
    Team = var.team_tag
  }
}

resource "aws_subnet" "subnet_1" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "${local.name}-subnet-1"
    Team = var.team_tag
  }
}

resource "aws_subnet" "subnet_2" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "${var.region}b"
  map_public_ip_on_launch = true

  tags = {
    Name = "${local.name}-subnet-2"
    Team = var.team_tag
  }
}

resource "aws_internet_gateway" "igw_1" {
  vpc_id = aws_vpc.vpc_1.id

  tags = {
    Name = "${local.name}-internet-gateway"
    Team = var.team_tag
  }
}

resource "aws_route_table" "rt_1" {
  vpc_id = aws_vpc.vpc_1.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw_1.id
  }

  tags = {
    Name = "${local.name}-routing-table"
    Team = var.team_tag
  }
}

resource "aws_route_table_association" "association_1" {
  subnet_id      = aws_subnet.subnet_1.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_route_table_association" "association_2" {
  subnet_id      = aws_subnet.subnet_2.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_security_group" "sg_1" {
  name = "${local.name}-security-group"

  # ingress -> 들어오는 트래픽
  # egress -> 나가는 트래픽
  # HTTP
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTPS
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Prometheus
  ingress {
    from_port   = 9090
    to_port     = 9090
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Grafana
  ingress {
    description = "Grafana"
    from_port   = 3300
    to_port     = 3300
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # k6
  ingress {
    description = "k6"
    from_port   = 5665
    to_port     = 5665
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Nginx Proxy Manager Admin"
    from_port   = 81
    to_port     = 81
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  vpc_id = aws_vpc.vpc_1.id

  tags = {
    Name = "${local.name}-security-group"
    Team = var.team_tag
  }
}

# EC2 설정 시작

# EC2 역할 생성
resource "aws_iam_role" "ec2_role_1" {
  name = "${local.name}-ec2-role-1"

  # 이 역할에 대한 신뢰 정책 설정. EC2 서비스가 이 역할을 가정할 수 있도록 설정
  assume_role_policy = <<EOF
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Sid": "",
        "Action": "sts:AssumeRole",
        "Principal": {
            "Service": "ec2.amazonaws.com"
        },
        "Effect": "Allow"
      }
    ]
  }
  EOF
}

# # EC2 역할에 AmazonS3FullAccess 정책을 부착
# resource "aws_iam_role_policy_attachment" "s3_full_access" {
#   role       = aws_iam_role.ec2_role_1.name
#   policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
# }

# EC2 역할에 AmazonEC2RoleforSSM 정책을 부착 (SSH 대체)
resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2_role_1.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM"
}

# IAM 인스턴스 프로파일 생성
resource "aws_iam_instance_profile" "instance_profile_1" {
  name = "${local.name}-instance-profile-2"
  role = aws_iam_role.ec2_role_1.name
}

# Elastic IP
resource "aws_eip" "ec2" {
  domain = "vpc"

  tags = {
    Name = "${local.name}-eip"
  }
}

locals {
  ec2_user_data_base = <<-END_OF_FILE
#!/bin/bash
# 가상 메모리 4GB 설정
sudo dd if=/dev/zero of=/swapfile bs=128M count=32
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
sudo sh -c 'echo "/swapfile swap swap defaults 0 0" >> /etc/fstab'

# 도커 설치 및 실행/활성화
yum install docker -y
systemctl enable docker
systemctl start docker

# 도커 네트워크 생성
docker network create common

# nginx 설치
docker run -d \
  --name npm_1 \
  --restart unless-stopped \
  --network common \
  -p 80:80 \
  -p 443:443 \
  -p 81:81 \
  -e TZ=Asia/Seoul \
  -v /dockerProjects/npm_1/volumes/data:/data \
  -v /dockerProjects/npm_1/volumes/etc/letsencrypt:/etc/letsencrypt \
  jc21/nginx-proxy-manager:latest

# haproxy 설치 (블루-그린 무중단 배포)
mkdir -p /dockerProjects/ha_proxy_1/volumes/usr/local/etc/haproxy

cat > /dockerProjects/ha_proxy_1/volumes/usr/local/etc/haproxy/haproxy.cfg <<'EOF'
global
    log stdout format raw local0
    maxconn 4096

defaults
    log global
    mode http
    option httplog
    timeout connect 5s
    timeout client 60s
    timeout server 60s

frontend http_front
    bind *:80
    acl host_api hdr(host) -i api.waitfair.shop
    use_backend blue_backend if host_api

# Blue 백엔드
backend blue_backend
    option httpchk GET /actuator/health
    http-check expect status 200
    default-server inter 2s rise 2 fall 3
    server blue app1:8080 check

# Green 백엔드
backend green_backend
    option httpchk GET /actuator/health
    http-check expect status 200
    default-server inter 2s rise 2 fall 3
    server green app1_temp:8080 check
EOF

docker run -d \
  --name ha_proxy_1 \
  --restart unless-stopped \
  --network common \
  -p 80:80 \
  -e TZ=Asia/Seoul \
  -v /dockerProjects/ha_proxy_1/volumes/usr/local/etc/haproxy:/usr/local/etc/haproxy:ro \
  haproxy:2.9-alpine


# redis 설치
docker run -d \
  --name=redis_1 \
  --restart unless-stopped \
  --network common \
  -p 6379:6379 \
  -e TZ=Asia/Seoul \
  redis --requirepass ${var.password_1}

echo "${var.github_access_token_1}" | docker login ghcr.io -u ${var.github_access_token_1_owner} --password-stdin

END_OF_FILE
}

# 단일 EC2 인스턴스 생성
resource "aws_instance" "ec2" {
  # 사용할 AMI ID
  ami = "ami-062cddb9d94dcf95d"
  # EC2 인스턴스 유형
  instance_type = var.instance_type
  # 사용할 서브넷 ID
  subnet_id = aws_subnet.subnet_1.id
  # 적용할 보안 그룹 ID
  vpc_security_group_ids = [aws_security_group.sg_1.id]
  # 퍼블릭 IP 연결 설정
  associate_public_ip_address = true

  # 인스턴스에 IAM 역할 연결
  iam_instance_profile = aws_iam_instance_profile.instance_profile_1.name

  # 인스턴스에 태그 설정
  tags = {
    Name = "${local.name}-ec2-1"
    Team = var.team_tag

  }

  # 루트 볼륨 설정
  root_block_device {
    volume_type = "gp3"
    volume_size = var.root_volume_size # 볼륨 크기를 12GB로 설정
  }

  user_data = <<-EOF
${local.ec2_user_data_base}
EOF
}

# Elastic IP Association
resource "aws_eip_association" "ec2" {
  instance_id   = aws_instance.ec2.id
  allocation_id = aws_eip.ec2.id
}
