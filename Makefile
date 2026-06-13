.PHONY: run build smoke clean zip docker-build docker-run

PORT ?= 8080
ZIP_NAME ?= public-records-search-java-github-ready.zip

run:
	./run.sh $(PORT)

build:
	javac src/PublicRecordsSearchServer.java

smoke:
	./ci-smoke-test.sh

clean:
	rm -f src/*.class $(ZIP_NAME)

zip: clean
	zip -r $(ZIP_NAME) . \
		-x '.git/*' \
		-x './.git/*' \
		-x '*.class' \
		-x '$(ZIP_NAME)' \
		-x '.arena/*' \
		-x '.cache/*'

docker-build:
	docker build -t public-records-search .

docker-run:
	docker run --rm -p $(PORT):8080 public-records-search
