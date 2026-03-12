```shell
docker run -d \
--name rabbitmq \
--hostname rabbitmq \
-p 5672:5672 \
-p 15672:15672 \
-e RABBITMQ_DEFAULT_USER=admin \
-e RABBITMQ_DEFAULT_PASS=123456 \
--restart unless-stopped \
rabbitmq:4-management
```

```shell
docker run -d \
  --name mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=123456 \
  --restart unless-stopped \
  mongo:latest
```