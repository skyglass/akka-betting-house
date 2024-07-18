#!/bin/bash

if [ -f ./env/.env.local ]; then
  set -o allexport
  source ./env/.env.local
  set +o allexport
else
  echo ".env.local file not found"
  exit 1
fi

set -u # or set -o nounset
: "$JWT_KEY"
: "$STRIPE_KEY"
: "$BASE_URL"

kubectl delete secret stripe-secret
kubectl delete secret jwt-secret
kubectl delete configmap base-url-config
kubectl create secret generic stripe-secret --from-literal=STRIPE_KEY=$STRIPE_KEY
kubectl create secret generic jwt-secret --from-literal=JWT_KEY=$JWT_KEY
kubectl create configmap base-url-config --from-literal=BASE_URL=$BASE_URL

# Temporary directory for the processed manifests
GENERATED_DIR=./k8s/generated
rm -rf $GENERATED_DIR
mkdir $GENERATED_DIR

# Process each manifest
for file in ./k8s/* ./k8s/local/*; do
  if [ -d "$file" ]; then
    continue
  fi
  envsubst < "$file" > "$GENERATED_DIR/$(basename "$file")"
done

envsubst < "./skaffold-template.yaml" > "./skaffold.yaml"