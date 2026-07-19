#!/bin/bash

mkdir -p ./certs
mkcert --install -key-file ./certs/local.key -cert-file ./certs/local.crt dihcord.com
