### 📖 Cloud-Native Startup Template

<ul style="list-style-type:disc">
  <li>📖 This <b>Cloud-Native Full-Stack Developer Template</b> provides fully functional Development and Production Environment</li>
    <li>📖 <b>Next.js</b> and <b>React</b> UI</li>
    <li>📖 <b>Node.js Typescript</b> Microservices</li>
    <li>📖 <b>Event-Driven</b> Microservices with Data Replication and Concurrency Control</li>
    <li>📖 Light-weight and high-performance <b>NATS</b> Messaging System</li>
    <li>📖 Local <b>Kubernetes</b> Development Environment with <b>Skaffold</b></li>
    <li>📖 Production <b>Kubernetes</b> Development Environment with <b>Skaffold</b></li>
    <li>📖 <b>Github Actions</b> CI/CD <b>GitOps</b> pipeline</li>
    <li>📖 <b>Azure Terraform</b> Infrastructure with <b>AKS Kubernetes Cluster</b> and <b>Private Container Registry</b></li>
  <li>📖 Full <b>Technology Stack</b>:</li>
  <ul>
    <li>✅ <b>React UI</b></li>  
    <li>✅ <b>Next.js React Framework</b></li>
    <li>✅ <b>Node.js Typescript Server</b></li>
    <li>✅ <b>Event-Driven Microservices with Data Replication and Concurrency Control</b></li>     
    <li>✅ <b>MongoDB Database</b></li>
    <li>✅ <b>Mongoose MongoDB Object Modeling for Node.js</b></li> 
    <li>✅ <b>NATS Messaging System</b></li>
    <li>✅ <b>Custom Authentication Service with JWT Tokens</b></li>
    <li>✅ <b>Custom Authorization Server</b></li>
    <li>✅ <b>Stripe Payment Infrastructure</b></li>   
    <li>✅ <b>Terraform</b></li>
    <li>✅ <b>Kubernetes</b></li>
    <li>✅ <b>Github Actions</b></li>
    <li>✅ <b>Github Secrets and envsubst Environment Variables parser</b></li>
    <li>✅ <b>Kubernetes Secrets and Configmap Variables</b></li>
    <li>✅ <b>Local Kubernetes Development Environment with Skaffold</b></li>
    <li>✅ <b>Production Kubernetes Development Environment with Skaffold</b></li>
    <li>✅ <b>Custom Kubernetes Manfiests Generation for Local and Production Environments with sh scripts</b></li>
    <li>✅ <b>Custom Skaffold Manifests Generation for Local and Production Environments with sh scripts</b></li>
    <li>✅ <b>Hot reload of Node.js Typescript for Local and Production Environments with Skaffold</b></li>
    <li>✅ <b>Hot reload of Docker Containers for Local and Production Environments with Skaffold</b></li>
  </ul>
</ul>

### 📖 Links

- `Microservices with Node JS and React` Udemy Course: https://www.udemy.com/course/microservices-with-node-js-and-react


## 📖 Step By Step Guide

### Step 01 - Create New Github Repository

- Clone this repository and copy the source code to your new repository

### Step-02: Prepare Your Azure Account

- make sure you have your own Azure Account with enough permissions (Sign Up for a Free Trial, if you don't have one)

### Step-03: Prepare Your Github Account

- make sure you have your own Github Account

### Step-04: Prepare Source Code and Github Actions Workflow:

- Edit "**.github/workflows/deploy-*.yaml**" files: replace "**master**" with the name of your main branch (you can change default main branch name in github repository settings)

- Edit "**k8s/prod/ingress-srv.yaml**" file: replace "**skycomposer.net**" with the name of your registered domain (see **Step-05**  and **Azure Production Environment Setup** for more details)


### Step-05: Register your domain:

- You need a registered domain to provide TLS connection with trusted Certificate Authority.

- For more details on setting up TLS on AKS Ingress with LetsEncrypt see this article: https://medium.com/@jainchirag8001/tls-on-aks-ingress-with-letsencrypt-f42d65725a3
This article will show you how to configure TLS on AKS with LetsEncrypt for any registered domain, including AWS Route 53.

- Make sure that you know how to create Hosted Zone and Record A for your domain provider.

- For more details, see `Azure Production Environment Setup`

### Step-06: Finish Udemy Course "Microservices with Node JS and React":

- If you need help on Microservices with Node JS and React, see more details in this course: https://www.udemy.com/course/microservices-with-node-js-and-react
- I strongly recommend you finish this course first, before following this guide!
- This guide will only help you deploy the microservices to azure cloud kubernetes cluster, enable github actions cd pipeline and configure local and production kubernetes development environment with skaffold
- All information about Next.js React Development, Node.js Typescript Development, Event-Driven Microservices with NATS Messaging System, Data Replication and Concurrency Control for Microservices, configuring custom Authentication Service and Authorization Server with JWT Tokens, and so on, is perfectly explained in this course!


## Local Kubernetes Environment Setup with Skaffold:

- Create local Kubernetes Cluster. If you have Docker Desktop, just go to Settings -> Kubernetes -> Enable Kubernetes ->  Apply & Restart

- Switch context to local Kubernetes Cluster. If you have Docker Desktop, just go to Kubernetes Context and select "docker-desktop"

```
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.4.0/deploy/static/provider/cloud/deploy.yaml
```
These commands will install nginx ingress controller to your local kubernetes cluster. You need nginx ingress controller for your local kubernetes ingress resource to work correctly (see `k8s/local/ingress-srv.yaml`for more information on your local kubernetes ingress resource)

- create `env` folder in the root of the project

- create `.env.local` file in `env` folder and provide the following parameters:

```
CONTAINER_REGISTRY="eventbooking.azurecr.io" (provide your own container registry, see **Azure Production Environment Setup** for more details)
DOCKER_FILE_NAME="Dockerfile"
DOCKER_PUSH="false"
VERSION="latest"
BASE_URL="http://ingress-nginx-controller.ingress-nginx.svc.cluster.local"

JWT_KEY="$JWT_KEY"
STRIPE_KEY="$STRIPE_KEY"
```

- Don't Worry! `env` folder is included to .gitignore. You will not reveal your secrets with git commit! :)
- JWT_KEY can be generated with the command `openssl rand -base64 32`
- STRIPE_KEY can be found in your Stripe Account (Developers -> API Keys -> Secret Key ->  Reveal test key)
- Note: CONTAINER_REGISTRY for local development environment can be any prefix, but it is recommended to use container registry name for consistency with production environment
- BASE_URL for local kubernetes cluster uses ingress-nginx service ip. Please, don't change it! If your nginx-ingress controller is installed correctly, this url will work as expected.

```
sh skaffold-local.sh
```
This script will build docker images and start local kubernetes environment with hot reloading of your code changes


- open `localhost` in your Browser and make sure that `Sign Up` and `Sign In` works, you are able to `Create a Ticket` and buy it

- optionally, create 2 test accounts, create ticket with one account and buy ticket with another account

- Use "magic" payment card with number 4242 4242 4242 4242 for unlimited payment. :)

- If the payment is sucessful, you will see your order with status `complete` in `Orders` tab

- Note: if the payment is successfull, you will not see ticket in `Tickets` tab anymore! All tickets in this app have a quantity of one! It means that only one user can buy a ticket! You can test concurrency control by trying to buy the same ticket with several users. First user will succeed, others will fail to buy a ticket!

- Congratulations! You successfuly tested `Ticketing App` locally!


## Azure Production Kubernetes Environment Setup with Skaffold:

- create `terraform.auto.tfvars` file in `infra` folder and provide following parameters:

```
kubernetes_version= "1.29.2"
app_name = "{provide_your_own_globally_unique_name}" 
location = "westeurope" (use any other azure location, for example, "germanywestcentral", if you have any issues with "westeurope")
```

- login to Azure Cloud with `az login` CLI

- cd to `infra` folder

- replace `eventbooking` with your own globally unique name (see files `container-registry.tf`, `kubernetes-cluster.tf` and `resource-group.tf`)

- run `terraform init`and `terraform apply --auto-approve`

- after the script is successfully finished, run the following command:

```
az aks get-credentials --resource-group {app_name} --name {app_name}
```

- Make sure that your context is switched from local Kubernetes Cluster to Azure Kubernetes Cluster. If you have Docker Desktop, just open Kubernetes Context and make sure that the name of the context corresponds to your Azure Kubernetes Cluster

- run `kubectl get pods` and make sure that `kubectl` works correctly and returns 0 resources

- login to Azure Container registry with the following command:

```
docker login {login_server}
```

- you can find docker login server, username and password in Azure Cloud (go to Container Registry -> Settings -> Access Keys)

- create `env` folder in the root of the project

- in `env` folder create `.env.prod` file and set the following environment variables:

```
CONTAINER_REGISTRY="eventbooking.azurecr.io"  (provide your own globally unique container registry)
DOCKER_FILE_NAME="Dockerfile-prod"
DOCKER_PUSH="true"
VERSION="latest"
BASE_URL="https://skycomposer.net" (provide your own domain name, see `Step-05` and notes below for more details)

JWT_KEY="$JWT_KEY"
STRIPE_KEY="$STRIPE_KEY"
```

- Don't Panic! `env` folder is included to .gitignore. You will not reveal your secrets with git commit! :)
- Make sure you set your own values for CONTAINER_REGISTRY, BASE_URL, JWT_KEY and STRIPE_KEY
- JWT_KEY can be generated with the command `openssl rand -base64 32`
- STRIPE_KEY can be found in your Stripe Account (Developers -> API Keys -> Secret Key ->  Reveal test key)
 
- register your domain and enable TLS on AKS Ingress with Lestencrypt: https://medium.com/@jainchirag8001/tls-on-aks-ingress-with-letsencrypt-f42d65725a3
- Make sure you provide your email for CA cluster issuer Kubernetes resource (see more details in the article)
- Make sure you installed ingress controller with helm (see more details in the article)
- Make sure you installed all other kubernetes resources and followed other instructions in the article
- You can find production Ingress Kubernetes Resource in `k8s/prod/ingress-srv.yaml`. This resource will be applied with `skaffold-prod.sh` or `skaffold-dev.sh` scripts. Make sure that you replaced `skycomposer.net` with your registered domain name

- run `sh skaffold-dev.sh`

- this script will build docker images, push them to azure container registry and deploy images to production kubernetes cluster with hot reloading of your code changes

- run `kubectl get pods` and make sure that all containers are RUNNING

- open https url with your registered domain in your Browser and make sure that `Sign Up` and `Sign In` works, you are able to `Create a Ticket` and buy it

- optionally, create 2 test accounts, create ticket with one account and buy ticket with another account

- Use "magic" payment card with number 4242 4242 4242 4242 for unlimited payment. :)

- If the payment is sucessful, you will see your order with status `complete` in `Orders` tab

- Note: if the payment is successfull, you will not see ticket in `Tickets` tab anymore! All tickets in this app have a quantity of one! It means that only one user can buy a ticket! You can test concurrency control by trying to buy the same ticket with several users. First user will succeed, others will fail to buy a ticket!

- Congratulations! You successfuly tested `Ticketing App` in production!

- run `sh skaffold-prod.sh` to deploy final changes to production

- The only difference between `sh skaffold-prod.sh` and `sh skaffold-dev.sh` is that `sh skaffold-dev.sh` allows hot reloading of your code changes on production! Try to make any code change with your IDE and you will immediately see this change on production!

- If you run `sh skaffold-dev.sh` you will see logs in real-time. After closing the cli window, all kubernetes resources will be destroyed! Therefore, in order to deploy final changes to production use `sh skaffold-prod.sh`. You will not have hot reloading with `sh skaffold-prod.sh`, but kubernetes resources will not be destroyed after you close cli window.

## Github Actions Deployment Pipeline Setup

- create the following Github Secrets (Go to Your Repository -> Settings -> Secrets and Variables -> Actions -> New Repository Secret):

```
CONTAINER_REGISTRY=... (Azure Container Registry)
KUBE_CONFIG=.. (Base64 encoded  ~/.kube/config file contents)
REGISTRY_UN=... (Azure Container Registry Username)
REGISTRY_PW=... (Azure Container Registry Password)
```

- you can find values for CONTAINER_REGISTRY, REGISTRY_UN and REGISTRY_PW in Azure Cloud (go to Container Registry -> Settings -> Access Keys)
- you can get the value of KUBE_CONFIG with this command `cat ~/.kube/config | base64` (make sure you switched context to Azure Production Kubernetes Cluster before running this command!)

- make any code changes (for example change `SkyComposer` to `SkyComposer 2` in `client/components/header.js` file)

- push changes with `git add .`, `git commit -m "test changes"`and `git push origin`

- go to "Your repository -> Actions" and make sure that the Deployment Pipeline is automatically started and successfully finished

- this pipeline will build changed docker image, push it to container registry and deploy changed image with new version to kubernetes cluster

- open https link for your registered domain in your Browser and make sure that you can see `SkyComposer 2` title on the top left

- Congratulations! You successfuly tested `Ticketing App` code changes with Github Actions Deployment Pipeline!