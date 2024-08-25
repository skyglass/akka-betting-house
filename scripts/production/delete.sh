kubectl delete -f ../../k8s/generated

kubectl delete configmap base-url-config
kubectl delete -f 'https://strimzi.io/install/latest?namespace=default'
kubectl delete -f 'https://raw.githubusercontent.com/strimzi/strimzi-kafka-operator/main/examples/kafka/kafka-persistent-single.yaml'