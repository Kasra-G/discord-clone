#!/bin/bash

mkdir -p ./certs
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout ./certs/local.key \
  -out ./certs/local.crt \
  -subj "/C=US/ST=State/L=City/O=Development/CN=dihcord.com" \
  -addext "subjectAltName = DNS:dihcord.com, DNS:://dihcord.com"
