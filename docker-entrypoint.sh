#!/bin/sh
set -e

# Ensure upload directories exist and are owned by the spring user.
# This fixes the case where a Docker bind-mount overlays /app/uploads
# with a host directory that is root-owned, preventing the spring user
# from writing files at runtime.
mkdir -p /app/uploads/po /app/uploads/products
chown -R spring:spring /app/uploads

# Drop from root to the spring user and start the application.
exec su-exec spring sh -c 'exec java $JAVA_OPTS -jar /app/app.jar'
