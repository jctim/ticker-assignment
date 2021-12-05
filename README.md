
Set up
------
```shell
$ docker-compose up -d
$ docker-compose exec broker kafka-topics --bootstrap-server broker:9092 --create --topic tickers
```