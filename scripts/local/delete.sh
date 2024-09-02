kubectl delete -f ../../k8s/generated

kubectl delete configmap base-url-config
kubectl delete -f 'https://strimzi.io/install/latest?namespace=default'

kubectl delete -f 'https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.4.0/deploy/static/provider/cloud/deploy.yaml'