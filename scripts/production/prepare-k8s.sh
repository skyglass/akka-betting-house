#!/bin/bash

if [ -f ./env/.env.prod ]; then
  set -o allexport
  source ./env/.env.prod
  set +o allexport
else
  echo ".env.prod file not found"
  exit 1
fi

set -u # or set -o nounset
: "$BASE_URL"
: "$API_BASE_URL"
: "$KEYCLOAK_BASE_URL"

kubectl delete configmap base-url-config
kubectl create configmap base-url-config --from-literal=BASE_URL=$BASE_URL --from-literal=API_BASE_URL=$API_BASE_URL --from-literal=KEYCLOAK_BASE_URL=$KEYCLOAK_BASE_URL

kubectl apply -f 'https://strimzi.io/install/latest?namespace=default'

helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
kubectl apply -f 'https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.4.0/deploy/static/provider/cloud/deploy.yaml'

# Temporary directory for the processed manifests
GENERATED_DIR=./k8s/generated
rm -rf $GENERATED_DIR
mkdir $GENERATED_DIR

# Process each manifest
for file in ./k8s/* ./k8s/prod/*; do
  if [ -d "$file" ]; then
    continue
  fi
  envsubst < "$file" > "$GENERATED_DIR/$(basename "$file")"
done

envsubst < "./skaffold-template.yaml" > "./skaffold.yaml"